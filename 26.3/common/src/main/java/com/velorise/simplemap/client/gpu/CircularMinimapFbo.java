package com.velorise.simplemap.client.gpu;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.velorise.simplemap.client.MapDebugRecorder.MapPipelineTelemetry;
import com.velorise.simplemap.client.MapRenderPlan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Optional;

/**
 * Pure Blaze3D offscreen circular minimap renderer. Clips each visible atlas quad
 * against a 32-gon inscribed circle via Sutherland-Hodgman polygon clipping, renders
 * into a transparent TextureTarget in a single RenderPass, and blits the result to GUI.
 */
public final class CircularMinimapFbo {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final int FBO_SIZE = 256;
    public static final Identifier TEXTURE_ID =
            Identifier.fromNamespaceAndPath("simplemap", "dynamic/minimap_circle_fbo");

    private static final int CIRCLE_SIDES = 32;
    private static final int MAX_DRAWS = 512;
    private static final float[] CLIP_NX = new float[CIRCLE_SIDES];
    private static final float[] CLIP_NY = new float[CIRCLE_SIDES];
    private static final float[] CLIP_D = new float[CIRCLE_SIDES];
    private static final float HALF_FBO = FBO_SIZE * 0.5f;
    private static final float CLIP_RADIUS = HALF_FBO - 1.5f;

    static {
        for (int i = 0; i < CIRCLE_SIDES; i++) {
            double a0 = (2.0 * Math.PI * i) / CIRCLE_SIDES;
            double a1 = (2.0 * Math.PI * ((i + 1) % CIRCLE_SIDES)) / CIRCLE_SIDES;
            float x0 = HALF_FBO + (float) Math.cos(a0) * CLIP_RADIUS;
            float y0 = HALF_FBO + (float) Math.sin(a0) * CLIP_RADIUS;
            float x1 = HALF_FBO + (float) Math.cos(a1) * CLIP_RADIUS;
            float y1 = HALF_FBO + (float) Math.sin(a1) * CLIP_RADIUS;
            // Inward-pointing unit normal for CCW circle edge
            float dx = x1 - x0;
            float dy = y1 - y0;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float nx = -dy / len;
            float ny = dx / len;
            CLIP_NX[i] = nx;
            CLIP_NY[i] = ny;
            CLIP_D[i] = nx * x0 + ny * y0;
        }
    }

    private static final CircularMinimapFbo INSTANCE = new CircularMinimapFbo();
    public static CircularMinimapFbo getInstance() { return INSTANCE; }

    private TextureTarget fbo;
    private FboTextureWrapper textureWrapper;
    private GpuBuffer vertexBuffer;
    private long vertexBufferCapacity = 0L;
    private GpuSampler linearSampler;
    private Projection orthoProjection;
    private ProjectionMatrixBuffer orthoProjectionBuffer;
    private ByteBufferBuilder scratchByteBuf;
    private boolean errorLogged = false;

    // Reusable batch and polygon clipping scratch buffers (render thread only)
    private final AbstractTexture[] drawTextures = new AbstractTexture[MAX_DRAWS];
    private final int[] drawStartQuad = new int[MAX_DRAWS];
    private final int[] drawQuadCount = new int[MAX_DRAWS];
    private int drawCount = 0;
    private int totalQuads = 0;
    private boolean frameHadMissingTexture = false;

    private final float[] polyX0 = new float[64];
    private final float[] polyY0 = new float[64];
    private final float[] polyU0 = new float[64];
    private final float[] polyV0 = new float[64];
    private final float[] polyX1 = new float[64];
    private final float[] polyY1 = new float[64];
    private final float[] polyU1 = new float[64];
    private final float[] polyV1 = new float[64];

    private final Matrix4f identityMat = new Matrix4f();
    private final Vector4f whiteMod = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private final Vector3f zeroOffset = new Vector3f(0.0f, 0.0f, 0.0f);

    // Retained dirty-key state so unchanged frames reuse the FBO with a single blit()
    private boolean fboValid = false;
    private MapRenderPlan cachedPlan;
    private MapRenderPlan cachedFallbackPlan;
    private long cachedPageTableGen = Long.MIN_VALUE;
    private int cachedSize = -1;
    private long cachedQuantCenterX = Long.MIN_VALUE;
    private long cachedQuantCenterZ = Long.MIN_VALUE;
    private float cachedScale = Float.NaN;
    private int cachedRotBucket = Integer.MIN_VALUE;
    private boolean cachedCaveMode = false;
    private int cachedBrightBucket = Integer.MIN_VALUE;
    private int cachedGlowBucket = Integer.MIN_VALUE;

    private CircularMinimapFbo() {}

    private void ensureInitialized() {
        if (fbo != null) return;
        // Allocate depth attachment so GUI_TEXTURED never warns about a missing depth texture
        fbo = new TextureTarget("simplemap_minimap_circle", FBO_SIZE, FBO_SIZE, GpuFormat.RGBA8_UNORM, GpuFormat.D32_FLOAT);
        linearSampler = RenderSystem.getDevice().createSampler(
                AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
        textureWrapper = new FboTextureWrapper(fbo, linearSampler);
        Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, textureWrapper);
    }

    private void ensureVertexBuffer(long requiredBytes) {
        if (vertexBuffer != null && vertexBufferCapacity >= requiredBytes) return;
        if (vertexBuffer != null) {
            vertexBuffer.close();
        }
        long alloc = Math.max(65536L, requiredBytes * 2L);
        vertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "simplemap_minimap_circle_vbo",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                alloc);
        vertexBufferCapacity = alloc;
    }

    public boolean renderAndBlit(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics,
            int viewportX, int viewportY, int size,
            MapRenderPlan plan,
            MapRenderPlan fallbackPlan,
            double centerX, double centerZ, float scale, float rotationRad,
            boolean caveMode, float baseBrightness, float glowStrength) {
        if (plan == null || size <= 0) return false;
        RenderSystem.assertOnRenderThread();
        try {
            ensureInitialized();
            if (fbo.getColorTextureView() == null) return false;

            float fboScale = (float) FBO_SIZE / (float) size;
            float effectiveScale = scale * fboScale;
            float bright = caveMode ? Math.max(0.05f, Math.min(1.0f, baseBrightness))
                    : Math.max(0.15f, Math.min(1.0f, baseBrightness));
            float glowAlpha = (!caveMode && glowStrength > 0.005f)
                    ? Math.max(0.0f, Math.min(1.0f, glowStrength)) : 0.0f;

            GpuPageTable.MapGpuPageTableService.RenderView pageTable =
                    GpuPageTable.MapGpuPageTableService.getInstance().renderView();
            long pageTableGen = pageTable.generation();
            long quantCenterX = Math.round(centerX * effectiveScale * 8.0);
            long quantCenterZ = Math.round(centerZ * effectiveScale * 8.0);
            int rotBucket = Math.round(rotationRad * 512.0f);
            int brightBucket = Math.round(bright * 64.0f);
            int glowBucket = glowAlpha > 0.0f ? Math.round(glowAlpha * 64.0f) : 0;

            boolean needsRedraw = !fboValid
                    || cachedPlan != plan
                    || cachedFallbackPlan != fallbackPlan
                    || cachedPageTableGen != pageTableGen
                    || cachedSize != size
                    || cachedQuantCenterX != quantCenterX
                    || cachedQuantCenterZ != quantCenterZ
                    || Float.compare(cachedScale, scale) != 0
                    || cachedRotBucket != rotBucket
                    || cachedCaveMode != caveMode
                    || cachedBrightBucket != brightBucket
                    || cachedGlowBucket != glowBucket;

            if (needsRedraw) {
                float cos = (float) Math.cos(rotationRad);
                float sin = (float) Math.sin(rotationRad);
                Minecraft mc = Minecraft.getInstance();

                drawCount = 0;
                totalQuads = 0;
                frameHadMissingTexture = false;

                if (scratchByteBuf == null) {
                    scratchByteBuf = new ByteBufferBuilder(65536);
                }
                MeshData mesh = null;
                boolean resetFailed = true;
                try {
                    BufferBuilder bb = new BufferBuilder(scratchByteBuf, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

                    if (fallbackPlan != null && fallbackPlan != plan) {
                        emitPlan(bb, fallbackPlan, pageTable, mc, false, centerX, centerZ,
                                effectiveScale, cos, sin, bright, 1.0f, 0.0f);
                    }
                    emitPlan(bb, plan, pageTable, mc, false, centerX, centerZ,
                            effectiveScale, cos, sin, bright, 1.0f, 1.0f);

                    if (glowAlpha > 0.0f) {
                        emitPlan(bb, plan, pageTable, mc, true, centerX, centerZ,
                                effectiveScale, cos, sin, 1.0f, glowAlpha, 2.0f);
                    }

                    mesh = totalQuads > 0 ? bb.build() : null;
                    CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                    if (mesh != null) {
                        ByteBuffer vbData = mesh.vertexBuffer();
                        ensureVertexBuffer(vbData.remaining());
                        encoder.writeToBuffer(vertexBuffer.slice(0L, vbData.remaining()), vbData);
                    }

                    GpuBufferSlice prevProj = RenderSystem.getProjectionMatrixBuffer();
                    ProjectionType prevProjType = RenderSystem.getProjectionType();
                    if (orthoProjection == null) {
                        orthoProjection = new Projection();
                        orthoProjection.setupOrtho(-1000.0f, 1000.0f, (float) FBO_SIZE, (float) FBO_SIZE, false);
                        orthoProjectionBuffer = new ProjectionMatrixBuffer("simplemap_minimap_circle_proj");
                    }
                    GpuBufferSlice projSlice = orthoProjectionBuffer.getBuffer(orthoProjection);
                    RenderSystem.setProjectionMatrix(projSlice, ProjectionType.ORTHOGRAPHIC);
                    try {
                        GpuBufferSlice dynSlice = RenderSystem.getDynamicUniforms().writeTransform(
                                identityMat, whiteMod, zeroOffset, identityMat);
                        RenderSystem.AutoStorageIndexBuffer seqIdx =
                                RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
                        GpuBuffer idxBuf = totalQuads > 0 ? seqIdx.getBuffer(totalQuads * 6) : null;
                        IndexType idxType = seqIdx.type();
                        GpuTextureView depthView = fbo.getDepthTextureView();

                        try (RenderPass pass = encoder.createRenderPass(
                                () -> "simplemap_minimap_circle",
                                fbo.getColorTextureView(),
                                Optional.of(new Vector4f(0.0f, 0.0f, 0.0f, 0.0f)),
                                depthView,
                                depthView != null ? OptionalDouble.of(1.0) : OptionalDouble.empty())) {
                            if (mesh != null && idxBuf != null) {
                                pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.GUI_TEXTURED));
                                RenderSystem.bindDefaultUniforms(pass);
                                pass.setUniform("DynamicTransforms", dynSlice);
                                pass.setVertexBuffer(0, vertexBuffer.slice());
                                pass.setIndexBuffer(idxBuf, idxType);
                                for (int i = 0; i < drawCount; i++) {
                                    AbstractTexture tex = drawTextures[i];
                                    if (tex == null || tex.getTextureView() == null) continue;
                                    pass.setUniform("Sampler0", tex.getTextureView(), tex.getSampler());
                                    pass.drawIndexed(drawQuadCount[i] * 6, 1, 0, drawStartQuad[i] * 4, 0);
                                }
                            }
                        }
                    } finally {
                        if (prevProj != null) {
                            RenderSystem.setProjectionMatrix(prevProj, prevProjType);
                        }
                    }
                    resetFailed = false;
                } finally {
                    if (mesh != null) mesh.close();
                    Arrays.fill(drawTextures, 0, drawCount, null);
                    drawCount = 0;
                    if (scratchByteBuf != null) {
                        if (resetFailed) {
                            scratchByteBuf.close();
                            scratchByteBuf = null;
                        } else {
                            scratchByteBuf.clear();
                        }
                    }
                }

                MapPipelineTelemetry.getInstance().recordRawBatchSubmissions(1);
                cachedPlan = plan;
                cachedFallbackPlan = fallbackPlan;
                cachedPageTableGen = pageTableGen;
                cachedSize = size;
                cachedQuantCenterX = quantCenterX;
                cachedQuantCenterZ = quantCenterZ;
                cachedScale = scale;
                cachedRotBucket = rotBucket;
                cachedCaveMode = caveMode;
                cachedBrightBucket = brightBucket;
                cachedGlowBucket = glowBucket;
                fboValid = !frameHadMissingTexture;
            }

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID,
                    viewportX, viewportY, 0.0f, 0.0f,
                    size, size, FBO_SIZE, FBO_SIZE, FBO_SIZE, FBO_SIZE);
            return true;
        } catch (Throwable t) {
            fboValid = false;
            if (!errorLogged) {
                errorLogged = true;
                LOGGER.error("CircularMinimapFbo failed during offscreen render", t);
            }
            return false;
        }
    }

    private void emitPlan(BufferBuilder bb, MapRenderPlan plan,
            GpuPageTable.MapGpuPageTableService.RenderView pageTable, Minecraft mc,
            boolean glow, double centerX, double centerZ, float effectiveScale,
            float cos, float sin, float brightness, float alpha, float z) {
        int[] phases = glow ? plan.glowPhases() : plan.basePhases();
        if (phases == null || phases.length == 0) return;
        GpuPageTable.MapGpuInstancePlan instancePlan = plan.instancePlan();
        List<MapRenderPlan.Batch> batches = glow ? plan.glowBatches() : plan.baseBatches();
        for (int phase : phases) {
            if (instancePlan != null) {
                emitInstancePhase(bb, instancePlan, pageTable, mc, phase,
                        centerX, centerZ, effectiveScale, cos, sin, brightness, alpha, z);
            }
            if (batches != null && !batches.isEmpty()) {
                emitBatchPhase(bb, batches, mc, phase,
                        centerX, centerZ, effectiveScale, cos, sin, brightness, alpha, z);
            }
        }
    }

    private void emitInstancePhase(BufferBuilder bb, GpuPageTable.MapGpuInstancePlan plan,
            GpuPageTable.MapGpuPageTableService.RenderView pageTable, Minecraft mc, int targetPhase,
            double centerX, double centerZ, float effectiveScale,
            float cos, float sin, float brightness, float alpha, float z) {
        int start = plan.firstIndexAtOrAfter(targetPhase);
        int end = plan.firstIndexAfter(targetPhase);
        for (int index = start; index < end && drawCount < MAX_DRAWS; index++) {
            GpuPageTable.PageTableEntry entry = pageTable.entry(plan.key(index));
            Identifier texId = pageTable.texture(entry);
            if (entry == null || texId == null || entry.atlasSize() <= 0) continue;
            int requiredCoverage = plan.requiredCoverageMask(index);
            int coverage = entry.coverageMask();
            boolean dynamicSurface = requiredCoverage == GpuPageTable.MapGpuInstancePlan.DYNAMIC_SURFACE_SUBTILES;
            if (!dynamicSurface && requiredCoverage != 0 && (coverage & requiredCoverage) != requiredCoverage) continue;
            if (dynamicSurface && (coverage & 0xFFFF) == 0) continue;

            AbstractTexture tex = mc.getTextureManager().getTexture(texId);
            if (tex == null || tex.getTextureView() == null) {
                frameHadMissingTexture = true;
                continue;
            }

            float invAtlas = 1.0f / (float) entry.atlasSize();
            float sourceSize = entry.sourceSize();
            float u0 = (entry.sourceX() + plan.localU0(index) * sourceSize) * invAtlas;
            float v0 = (entry.sourceY() + plan.localV0(index) * sourceSize) * invAtlas;
            float u1 = (entry.sourceX() + plan.localU1(index) * sourceSize) * invAtlas;
            float v1 = (entry.sourceY() + plan.localV1(index) * sourceSize) * invAtlas;

            float wx0 = plan.x(index);
            float wz0 = plan.y(index);
            float wx1 = wx0 + plan.width(index);
            float wz1 = wz0 + plan.height(index);

            emitClippedQuad(bb, tex, wx0, wz0, wx1, wz1, u0, v0, u1, v1,
                    centerX, centerZ, effectiveScale, cos, sin, brightness, alpha, z);
        }
    }

    private void emitBatchPhase(BufferBuilder bb, List<MapRenderPlan.Batch> batches,
            Minecraft mc, int targetPhase,
            double centerX, double centerZ, float effectiveScale,
            float cos, float sin, float brightness, float alpha, float z) {
        for (MapRenderPlan.Batch batch : batches) {
            if (batch.phase() != targetPhase) continue;
            if (drawCount >= MAX_DRAWS) return;
            AbstractTexture tex = mc.getTextureManager().getTexture(batch.texture());
            if (tex == null || tex.getTextureView() == null) {
                frameHadMissingTexture = true;
                continue;
            }
            float[] v = batch.vertices();
            for (int i = 0; i + 16 <= v.length && drawCount < MAX_DRAWS; i += 16) {
                emitClippedQuad(bb, tex,
                        v[i], v[i + 1], v[i + 8], v[i + 9],
                        v[i + 2], v[i + 3], v[i + 10], v[i + 11],
                        centerX, centerZ, effectiveScale, cos, sin, brightness, alpha, z);
            }
        }
    }

    private void emitClippedQuad(BufferBuilder bb, AbstractTexture tex,
            float wx0, float wz0, float wx1, float wz1,
            float u0, float v0, float u1, float v1,
            double centerX, double centerZ, float effectiveScale,
            float cos, float sin, float brightness, float alpha, float z) {
        if (drawCount >= MAX_DRAWS) return;
        // Transform 4 world corners into FBO pixel space (screen-CCW order)
        setTransformed(0, wx0, wz0, u0, v0, centerX, centerZ, effectiveScale, cos, sin);
        setTransformed(1, wx0, wz1, u0, v1, centerX, centerZ, effectiveScale, cos, sin);
        setTransformed(2, wx1, wz1, u1, v1, centerX, centerZ, effectiveScale, cos, sin);
        setTransformed(3, wx1, wz0, u1, v0, centerX, centerZ, effectiveScale, cos, sin);

        int n = clipPolygonToCircle(4);
        if (n < 3) return;

        int quadsAdded = n - 2;
        for (int k = 1; k < n - 1; k++) {
            // Under orthoInvertY=false, Y-up projection reflects screen-CCW vertices to CW in NDC;
            // emit [0, k+1, k, k] so triangles are CCW (front-facing) under GUI_TEXTURED culling.
            bb.addVertex(polyX0[0], polyY0[0], z).setUv(polyU0[0], polyV0[0]).setColor(brightness, brightness, brightness, alpha);
            bb.addVertex(polyX0[k + 1], polyY0[k + 1], z).setUv(polyU0[k + 1], polyV0[k + 1]).setColor(brightness, brightness, brightness, alpha);
            bb.addVertex(polyX0[k], polyY0[k], z).setUv(polyU0[k], polyV0[k]).setColor(brightness, brightness, brightness, alpha);
            bb.addVertex(polyX0[k], polyY0[k], z).setUv(polyU0[k], polyV0[k]).setColor(brightness, brightness, brightness, alpha);
        }

        if (drawCount > 0 && drawTextures[drawCount - 1] == tex) {
            drawQuadCount[drawCount - 1] += quadsAdded;
        } else {
            drawTextures[drawCount] = tex;
            drawStartQuad[drawCount] = totalQuads;
            drawQuadCount[drawCount] = quadsAdded;
            drawCount++;
        }
        totalQuads += quadsAdded;
    }

    private void setTransformed(int idx, float wx, float wz, float u, float v,
            double centerX, double centerZ, float effectiveScale, float cos, float sin) {
        float dx = (float) (wx - centerX) * effectiveScale;
        float dz = (float) (wz - centerZ) * effectiveScale;
        polyX0[idx] = HALF_FBO + dx * cos - dz * sin;
        polyY0[idx] = HALF_FBO + dx * sin + dz * cos;
        polyU0[idx] = u;
        polyV0[idx] = v;
    }

    private int clipPolygonToCircle(int count) {
        int n = count;
        for (int side = 0; side < CIRCLE_SIDES && n >= 3; side++) {
            float nx = CLIP_NX[side];
            float ny = CLIP_NY[side];
            float d = CLIP_D[side];
            int out = 0;
            float px = polyX0[n - 1];
            float py = polyY0[n - 1];
            float pu = polyU0[n - 1];
            float pv = polyV0[n - 1];
            float pDist = nx * px + ny * py - d;
            for (int i = 0; i < n; i++) {
                float cx = polyX0[i];
                float cy = polyY0[i];
                float cu = polyU0[i];
                float cv = polyV0[i];
                float cDist = nx * cx + ny * cy - d;
                if (cDist >= 0.0f) {
                    if (pDist < 0.0f && out < 63) {
                        float t = pDist / (pDist - cDist);
                        polyX1[out] = px + t * (cx - px);
                        polyY1[out] = py + t * (cy - py);
                        polyU1[out] = pu + t * (cu - pu);
                        polyV1[out] = pv + t * (cv - pv);
                        out++;
                    }
                    if (out < 63) {
                        polyX1[out] = cx;
                        polyY1[out] = cy;
                        polyU1[out] = cu;
                        polyV1[out] = cv;
                        out++;
                    }
                } else if (pDist >= 0.0f && out < 63) {
                    float t = pDist / (pDist - cDist);
                    polyX1[out] = px + t * (cx - px);
                    polyY1[out] = py + t * (cy - py);
                    polyU1[out] = pu + t * (cu - pu);
                    polyV1[out] = pv + t * (cv - pv);
                    out++;
                }
                px = cx; py = cy; pu = cu; pv = cv; pDist = cDist;
            }
            System.arraycopy(polyX1, 0, polyX0, 0, out);
            System.arraycopy(polyY1, 0, polyY0, 0, out);
            System.arraycopy(polyU1, 0, polyU0, 0, out);
            System.arraycopy(polyV1, 0, polyV0, 0, out);
            n = out;
        }
        return n;
    }

    private static final class FboTextureWrapper extends AbstractTexture {
        FboTextureWrapper(TextureTarget target, GpuSampler sampler) {
            this.texture = target.getColorTexture();
            this.textureView = target.getColorTextureView();
            this.sampler = sampler;
        }
    }
}
