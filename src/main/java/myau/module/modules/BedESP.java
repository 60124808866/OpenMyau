package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.util.RenderUtil;
import myau.property.properties.*;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.block.*;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArraySet;

public class BedESP extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final CopyOnWriteArraySet<BlockPos> beds = new CopyOnWriteArraySet<>();
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{"DEFAULT", "FULL"});
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"CUSTOM", "HUD"});
    public final ColorProperty customColor;
    public final PercentProperty opacity;
    public final BooleanProperty outline;
    public final FloatProperty outlineWidth;
    public final BooleanProperty expose;
    public final ColorProperty exposeColor;
    public final BooleanProperty obsidian;

    private Color getColor() {
        switch (this.color.getValue()) {
            case 0:
                return new Color(this.customColor.getValue());
            case 1:
                return ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
            default:
                return new Color(-1);
        }
    }

    private void drawObsidianBox(AxisAlignedBB axisAlignedBB) {
        int red = 170;
        int green = 0;
        int blue = 170;
        
        if (this.outline.getValue()) {
            RenderUtil.drawBoundingBox(axisAlignedBB, red, green, blue, 255, this.outlineWidth.getValue());
        }
        RenderUtil.drawFilledBox(axisAlignedBB, red, green, blue);
    }

    private void drawObsidian(BlockPos blockPos) {
        int red = 170;
        int green = 0;
        int blue = 170;
        
        if (this.outline.getValue()) {
            RenderUtil.drawBlockBoundingBox(blockPos, 1.0, red, green, blue, 255, this.outlineWidth.getValue());
        }
        RenderUtil.drawBlockBox(blockPos, 1.0, red, green, blue);
    }

    private boolean isBlockExposed(BlockPos headPos, BlockPos footPos) {
        for (EnumFacing facing : Arrays.asList(EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST)) {
            BlockPos headOffset = headPos.offset(facing);
            BlockPos footOffset = footPos.offset(facing);
            
            if (mc.theWorld.isAirBlock(headOffset) || mc.theWorld.isAirBlock(footOffset)) {
                return true;
            }
        }
        if (mc.theWorld.isAirBlock(headPos.up()) || mc.theWorld.isAirBlock(footPos.up())) {
            return true;
        }
        return false;
    }


    public BedESP() {
        super("BedESP", false);
        this.customColor = new ColorProperty("custom-color", (int) 8085714755840333141L, () -> this.color.getValue() == 0);
        this.opacity = new PercentProperty("opacity", 25);
        this.outline = new BooleanProperty("outline", false);
        this.outlineWidth = new FloatProperty("outline-width", 1.5F, 0.5F, 5.0F, () -> this.outline.getValue());
        this.expose = new BooleanProperty("expose", false, () -> this.outline.getValue());
        this.exposeColor = new ColorProperty("expose-color", 0x00D1A4, () -> this.outline.getValue() && this.expose.getValue());
        this.obsidian = new BooleanProperty("obsidian", true);
    }

    public double getHeight() {
        return this.mode.getValue() == 1 ? 1.0 : 0.5625;
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (this.isEnabled()) {
            RenderUtil.enableRenderState();
            for (BlockPos blockPos : this.beds) {
                IBlockState state = mc.theWorld.getBlockState(blockPos);
                if (state.getBlock() instanceof BlockBed && state.getValue(BlockBed.PART) == EnumPartType.HEAD) {
                    BlockPos opposite = blockPos.offset(state.getValue(BlockBed.FACING).getOpposite());
                    IBlockState oppositeState = mc.theWorld.getBlockState(opposite);
                    if (oppositeState.getBlock() instanceof BlockBed && oppositeState.getValue(BlockBed.PART) == EnumPartType.FOOT) {
                        boolean bedExposed = this.expose.getValue() && this.isBlockExposed(blockPos, opposite);
                        
                        if (this.obsidian.getValue()) {
                            for (EnumFacing facing : Arrays.asList(EnumFacing.UP, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST)) {
                                BlockPos offsetX = blockPos.offset(facing);
                                BlockPos offsetZ = opposite.offset(facing);
                                IBlockState stateX = mc.theWorld.getBlockState(offsetX);
                                IBlockState stateZ = mc.theWorld.getBlockState(offsetZ);
                                boolean xObsidian = stateX.getBlock() instanceof BlockObsidian;
                                boolean zObsidian = stateZ.getBlock() instanceof BlockObsidian;
                                
                                if (xObsidian && zObsidian) {
                                    this.drawObsidianBox(
                                            new AxisAlignedBB(
                                                    Math.min(offsetX.getX(), offsetZ.getX()),
                                                    offsetX.getY(),
                                                    Math.min(offsetX.getZ(), offsetZ.getZ()),
                                                    Math.max((double) offsetX.getX() + 1.0, (double) offsetZ.getX() + 1.0),
                                                    (double) offsetX.getY() + 1.0,
                                                    Math.max((double) offsetX.getZ() + 1.0, (double) offsetZ.getZ() + 1.0)
                                            )
                                                    .offset(
                                                            -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                                                            -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                                                            -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
                                                    )
                                    );
                                } else if (xObsidian) {
                                    this.drawObsidian(offsetX);
                                } else if (zObsidian) {
                                    this.drawObsidian(offsetZ);
                                }
                            }
                        }
                        AxisAlignedBB aabb = new AxisAlignedBB(
                                Math.min(blockPos.getX(), opposite.getX()),
                                blockPos.getY(),
                                Math.min(blockPos.getZ(), opposite.getZ()),
                                Math.max((double) blockPos.getX() + 1.0, (double) opposite.getX() + 1.0),
                                (double) blockPos.getY() + this.getHeight(),
                                Math.max((double) blockPos.getZ() + 1.0, (double) opposite.getZ() + 1.0)
                        )
                                .offset(
                                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(),
                                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(),
                                        -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()
                                );
                        
                        Color fillColor = this.getColor();
                        
                        if (this.outline.getValue()) {
                            Color outlineColor;
                            if (bedExposed) {
                                outlineColor = new Color(this.exposeColor.getValue());
                            } else {
                                outlineColor = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
                            }
                            RenderUtil.drawBoundingBox(aabb, outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), 255, this.outlineWidth.getValue());
                        }
                        
                        RenderUtil.drawFilledBox(
                                aabb,
                                fillColor.getRed(),
                                fillColor.getGreen(),
                                fillColor.getBlue()
                        );
                    }
                } else {
                    this.beds.remove(blockPos);
                }
            }
            RenderUtil.disableRenderState();
        }
    }

    @Override
    public void onEnabled() {
        if (mc.renderGlobal != null) {
            mc.renderGlobal.loadRenderers();
        }
    }
}