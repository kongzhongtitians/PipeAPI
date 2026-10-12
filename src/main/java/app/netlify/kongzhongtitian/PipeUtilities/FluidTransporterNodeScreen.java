package app.netlify.kongzhongtitian.PipeUtilities;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;

public class FluidTransporterNodeScreen extends AbstractContainerScreen<FluidTransporterNodeMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(PipeAPI.MODID, "textures/screen/lx.png");

    public FluidTransporterNodeScreen(FluidTransporterNodeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        gui.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // 升级槽底框（因为 dispenser 贴图此处没有槽位图形）
        int sx = leftPos + 134;
        int sy = topPos + 20;
        gui.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF373737);
        gui.fill(sx,     sy,     sx + 16, sy + 16, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float delta) {
        this.renderBackground(gui);
        super.render(gui, mouseX, mouseY, delta);

        FluidTransporterNodeBlockEntity be = this.menu.getBlockEntity();
        if (be != null) {
            FluidStack fluid = be.getFluidTank().getFluid();
            String fluidName = fluid.isEmpty()
                    ? Component.translatable("gui.pipe_api.fluid_empty").getString()
                    : fluid.getDisplayName().getString();
            int amount = be.getFluidTank().getFluidAmount();
            int capacity = be.getFluidTank().getCapacity();

            gui.drawString(this.font,
                    Component.translatable("gui.pipe_api.fluid_type", fluidName),
                    leftPos + 8, topPos + 20, 0xFFFFFF, false);
            gui.drawString(this.font,
                    Component.translatable("gui.pipe_api.fluid_amount", amount, capacity),
                    leftPos + 8, topPos + 34, 0xFFFFFF, false);

            // 新增：显示当前速度倍率
            double mult = be.getSpeedMultiplierPublic();
            String speedText = String.format("Speed: x%.2f", 1.0 / Math.max(mult, 1e-9));
            gui.drawString(this.font, speedText,
                    leftPos + 8, topPos + 48, 0xAAFFAA, false);
        }

        this.renderTooltip(gui, mouseX, mouseY);
    }
}