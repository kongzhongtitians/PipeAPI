package gd.rf.kongzhongtitian.PipeAPI;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class TransporterNodeScreen extends AbstractContainerScreen<TransporterNodeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PipeAPI.MODID, "textures/screen/xiang.png");

    private static final MutableComponent[] DIRECTION_SHORT = {Component.translatable("gui.pipe_api.east_short"), Component.translatable("gui.pipe_api.south_short"), Component.translatable("gui.pipe_api.west_short"), Component.translatable("gui.pipe_api.north_short"), Component.translatable("gui.pipe_api.up_short"),Component.translatable("gui.pipe_api.down_short")};

    // 槽位坐标 —— 与 TransporterNodeMenu 保持一致
    private static final int CACHE_X    = TransporterNodeMenu.CACHE_X;
    private static final int CACHE_Y    = TransporterNodeMenu.CACHE_Y;
    private static final int FILTER_X   = TransporterNodeMenu.FILTER_X;
    private static final int FILTER_Y   = TransporterNodeMenu.FILTER_Y;
    private static final int SPEED_X    = TransporterNodeMenu.SPEED_X;
    private static final int SPEED_Y    = TransporterNodeMenu.SPEED_Y;
    private static final int RESERVED_X = TransporterNodeMenu.RESERVED_X;
    private static final int RESERVED_Y = TransporterNodeMenu.RESERVED_Y;
    private static final int C_GREEN = 0x4000FF00;
    private static final int C_BLUE = 0x4022AAFF;
    private static final int C_OR = 0x40FFAA00;
    private static final int C_CYAN = 0x40AAFFFF;
    private static final int C_RED = 0x40FF0000;
    private static final int[] DIR_Y      = TransporterNodeMenu.DIR_Y;
    private static final int YPLUS      = 15;
    private static final int[] DIR_X    = TransporterNodeMenu.DIR_X;

    public TransporterNodeScreen(TransporterNodeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 210;
    }

    // -------- 工具方法 --------

    private void drawSlotFrame(GuiGraphics gui, int x, int y, int fill, int outline) {
        int px = leftPos + x;
        int py = topPos + y;
        gui.fill(px, py, px + 16, py + 16, fill);
        gui.renderOutline(px, py, 16, 16, outline);
    }

    /** 在槽位内部居中绘制一个小字母标识 */
    private void drawSlotLabel(GuiGraphics gui, int slotX, int slotY, MutableComponent label, int color) {
        int px = leftPos + slotX;
        int py = topPos + slotY;
        int w = this.font.width(label);
        gui.drawString(this.font, label,
                px + (16 - w) / 2, py + 4,
                color, false);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // ---- 缓存槽：绿色 ----
        drawSlotFrame(gui, CACHE_X, CACHE_Y, C_GREEN, 0xFF00FF00);

        // ---- 过滤器槽：橙色 ----
        drawSlotFrame(gui, FILTER_X, FILTER_Y, C_OR, 0xFFFFAA00);

        // ---- 速度升级槽：蓝色 ----
        drawSlotFrame(gui, SPEED_X, SPEED_Y, C_BLUE, 0xFF22AAFF);

        // ---- 预留槽（禁用）：灰色 ----
        drawSlotFrame(gui, RESERVED_X, RESERVED_Y, C_CYAN, 0xFFAAFFFF);

        // ---- 方向槽：红色（有火把=实心高亮，无火把=仅描边）----
        for (int i = 0; i < TransporterNodeBlockEntity.DIR_SLOT_COUNT; i++) {
            int x = DIR_X[i];
            ItemStack torch = this.menu.getSlot(
                    TransporterNodeBlockEntity.SLOT_DIR_START + i).getItem();
            if (!torch.isEmpty()) {
                drawSlotFrame(gui, x, DIR_Y[i], C_RED, 0xFFFF0000);
            } else {
                gui.renderOutline(leftPos + x, topPos + DIR_Y[i], 16, 16, 0x80FF0000);
            }
        }
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, delta);

        drawSlotLabel(gui, CACHE_X,    CACHE_Y+YPLUS,    Component.translatable("gui.pipe_api.c"), 0xFF888888);
        drawSlotLabel(gui, FILTER_X,   FILTER_Y+YPLUS,   Component.translatable("gui.pipe_api.f"), 0xFF888888);
        drawSlotLabel(gui, SPEED_X,    SPEED_Y+YPLUS,    Component.translatable("gui.pipe_api.s"), 0xFF888888);
        drawSlotLabel(gui, RESERVED_X, RESERVED_Y+YPLUS, Component.translatable("gui.pipe_api.r"), 0xFF888888);

        // ---- 方向槽下方的 E S W N U D 标注 ----
        for (int i = 0; i < TransporterNodeBlockEntity.DIR_SLOT_COUNT; i++) {
            MutableComponent label = DIRECTION_SHORT[i];
            int w = this.font.width(label);
            gui.drawString(this.font, label,
                    leftPos + DIR_X[i] + (16 - w) / 2,
                    topPos + DIR_Y[i] + (16 - w) / 2,
                    0xFFFFFFFF, false);
        }
        double speed = menu.getSpeedMultiplier();
        String text = String.format("Speed: %.4f", speed)+'x';
        gui.drawString(this.font,text,leftPos+FILTER_X-40,topPos+FILTER_Y+60,0xFF888888,false);

        // ---- 绘制完所有内容后，统一绘制 tooltip ----
        this.renderTooltip(gui, mouseX, mouseY);
    }

    private int getHoveredDirectionSlot(int mouseX, int mouseY) {
        for (int i = 0; i < TransporterNodeBlockEntity.DIR_SLOT_COUNT; i++) {
            int x = leftPos + DIR_X[i];
            int y = topPos + DIR_Y[i];
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void renderTooltip(GuiGraphics gui, int mouseX, int mouseY) {
        int dirIdx = getHoveredDirectionSlot(mouseX, mouseY);
        if (dirIdx >= 0) {
            final Direction dir = TransporterNodeBlockEntity.DIRECTION_ORDER[dirIdx];
            final List<Component> lines = new ArrayList<>();

            MutableComponent dirIn;
            switch (dirIdx) {
                case 0:  dirIn = Component.translatable("gui.pipe_api.east");  break;
                case 1:  dirIn = Component.translatable("gui.pipe_api.south"); break;
                case 2:  dirIn = Component.translatable("gui.pipe_api.west");  break;
                case 3:  dirIn = Component.translatable("gui.pipe_api.north"); break;
                case 4:  dirIn = Component.translatable("gui.pipe_api.up");    break;
                case 5:  dirIn = Component.translatable("gui.pipe_api.down");  break;
                default: dirIn = Component.literal("?");
            }
            lines.add(dirIn);

            this.menu.getAccess().evaluate((level, pos) -> {
                BlockPos neighbor = pos.relative(dir);
                if (!level.isLoaded(neighbor)) {
                    lines.add(Component.literal("§7[unloaded]"));
                } else {
                    BlockState state = level.getBlockState(neighbor);
                    if (state.isAir()) {
                        lines.add(Component.literal("§7[empty]"));
                    } else {
                        lines.add(Component.literal("§f").append(state.getBlock().getName()));
                    }
                }
                return TRUE;
            }, FALSE);

            ItemStack torch = this.menu.getSlot(
                    TransporterNodeBlockEntity.SLOT_DIR_START + dirIdx).getItem();
            if (!torch.isEmpty()) {
                lines.add(Component.translatable("gui.pipe_api.enabled"));
            } else {
                lines.add(Component.translatable("gui.pipe_api.disabled"));
            }

            gui.renderTooltip(this.font, lines, Optional.empty(), ItemStack.EMPTY, mouseX, mouseY);
            return;
        }

        // 非方向槽：交给默认逻辑（会显示缓存槽 / 过滤器 / 速度等物品的 tooltip）
        super.renderTooltip(gui, mouseX, mouseY);
    }
}