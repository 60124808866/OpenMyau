package myau.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.ItemSword;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SideOnly(Side.CLIENT)
@Mixin({ItemSword.class})
public interface IAccessorItemSword {
    @Accessor
    Item.ToolMaterial getMaterial();
}
