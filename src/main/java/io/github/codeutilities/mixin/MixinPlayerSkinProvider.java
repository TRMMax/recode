package io.github.codeutilities.mixin;

import com.google.common.hash.Hashing;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import java.io.File;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerSkinProvider.class)
public class MixinPlayerSkinProvider {

    @Shadow
    @Final
    private TextureManager textureManager;
    @Shadow
    @Final
    private File skinCacheDir;

    //Complete overwrite for loadSkin method in net.minecraft.client.texture.PlayerSkinProvider

    @Inject(method = "loadSkin(Lcom/mojang/authlib/minecraft/MinecraftProfileTexture;Lcom/mojang/authlib/minecraft/MinecraftProfileTexture$Type;Lnet/minecraft/client/texture/PlayerSkinProvider$SkinTextureAvailableCallback;)Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    private void loadSkin(MinecraftProfileTexture profileTexture, Type type,
        @Nullable PlayerSkinProvider.SkinTextureAvailableCallback callback,
        CallbackInfoReturnable<Identifier> cir) {

        String string = Hashing.sha1().hashUnencodedChars(profileTexture.getHash()).toString();
        Identifier identifier = new Identifier("skins/" + string);
        AbstractTexture abstractTexture = this.textureManager.getTexture(identifier);
        if (abstractTexture != null) {
            if (callback != null) {
                callback.onSkinTextureAvailable(type, identifier, profileTexture);
            }
        } else {
            File file = new File(skinCacheDir, string.length() > 2 ? string.substring(0, 2) : "xx");
            File file2 = new File(file, string);
            Identifier finalIdentifier = identifier;
            new Thread(() -> {
                PlayerSkinTexture playerSkinTexture = new PlayerSkinTexture(file2,
                    profileTexture.getUrl(), DefaultSkinHelper
                    .getTexture(), type == Type.SKIN, () -> {
                    if (callback != null) {
                        callback.onSkinTextureAvailable(type, finalIdentifier, profileTexture);
                    }

                });
                this.textureManager.registerTexture(finalIdentifier, playerSkinTexture);
            }).start();
            identifier = DefaultSkinHelper.getTexture();
        }

        cir.setReturnValue(identifier);
        cir.cancel();
    }

}