package net.wowmod.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.wowmod.WeaponsOfWar;

public record LongJumpPayload() implements CustomPayload {
    public static final CustomPayload.Id<LongJumpPayload> ID = new CustomPayload.Id<>(Identifier.of(WeaponsOfWar.MOD_ID, "long_jump"));

    // Since we are sending no data, we use PacketCodec.unit
    public static final PacketCodec<RegistryByteBuf, LongJumpPayload> CODEC = PacketCodec.unit(new LongJumpPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}