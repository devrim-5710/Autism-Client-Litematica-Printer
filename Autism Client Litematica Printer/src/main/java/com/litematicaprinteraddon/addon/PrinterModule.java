package com.litematicaprinteraddon.addon;

import autismclient.api.module.SimpleModule;
import autismclient.util.AutismContainerTarget;
import autismclient.util.AutismInventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PrinterModule extends SimpleModule {
    private int cooldown;

    public PrinterModule() {
        super("printer", "Litematica Printer", "Prints the schematic loaded in Litematica.");
        addDouble("range", "Reach", 4.5, 2.0, 8.0, 0.25);
        addBool("air-place", "Air Place", false);
        addInt("blocks-per-tick", "Blocks Per Tick", 1, 1, 16, 1);
        addInt("delay", "Delay Ticks", 0, 0, 40, 1);
        addBool("switch-back", "Restore Selected Slot", false);
    }

    @Override
    public void onEnable() {
        cooldown = 0;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || mc.level == null) return;
        if (mc.player.containerMenu != mc.player.inventoryMenu) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        BlockGetter schematic = LitematicaBridge.schematicWorld();
        if (schematic == null) {
            return;
        }

        double reach = getDouble("range");
        Vec3 eye = mc.player.getEyePosition();
        BlockPos eyePos = BlockPos.containing(eye);
        int r = (int) Math.ceil(reach);

        List<BlockPos> candidates = new ArrayList<>();
        for (int dy = -r; dy <= r; dy++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dx = -r; dx <= r; dx++) {
                    BlockPos pos = eyePos.offset(dx, dy, dz);
                    BlockState want = schematic.getBlockState(pos);
                    if (want.isAir()) continue;
                    BlockState current = mc.level.getBlockState(pos);
                    if (current == want || (!current.isAir() && !current.canBeReplaced())) continue;
                    candidates.add(pos);
                }
            }
        }
        if (candidates.isEmpty()) return;

        candidates.sort(Comparator.comparingDouble(p -> Vec3.atCenterOf(p).distanceTo(eye)));

        int budget = getInt("blocks-per-tick");
        int beforeSlot = mc.player.getInventory().getSelectedSlot();
        int placed = 0;
        for (BlockPos pos : candidates) {
            if (placed >= budget) break;
            BlockHitResult hit = findPlaceHit(mc, pos);
            if (hit == null && getBool("air-place")) {
                hit = airPlaceHit(mc, pos);
            }
            if (hit == null) continue;
            var item = schematic.getBlockState(pos).getBlock().asItem();
            if (item.getDefaultInstance().isEmpty()) continue;
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
            int slot = AutismInventoryHelper.selectHotbarItemByName(mc, itemId, beforeSlot);
            if (slot < 0) continue;
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            placed++;
        }

        if (placed > 0) {
            cooldown = getInt("delay");
            if (getBool("switch-back")) {
                AutismInventoryHelper.selectHotbarSlot(mc, beforeSlot);
            }
        }
    }

    private static BlockHitResult findPlaceHit(Minecraft mc, BlockPos targetPos) {
        if (mc.level.isOutsideBuildHeight(targetPos)) return null;
        BlockState target = mc.level.getBlockState(targetPos);
        if (!target.isAir() && !target.canBeReplaced()) return null;
        for (Direction face : Direction.values()) {
            BlockPos supportPos = targetPos.relative(face.getOpposite());
            if (mc.level.isOutsideBuildHeight(supportPos)) continue;
            if (mc.level.getBlockState(supportPos).isAir()) continue;
            if (!AutismContainerTarget.isWithinBlockReach(mc, supportPos)) continue;
            Vec3 hitPos = Vec3.atCenterOf(supportPos).add(
                face.getStepX() * 0.5D,
                face.getStepY() * 0.5D,
                face.getStepZ() * 0.5D
            );
            return new BlockHitResult(hitPos, face, supportPos, false);
        }
        return null;
    }

    private static BlockHitResult airPlaceHit(Minecraft mc, BlockPos targetPos) {
        if (mc.level.isOutsideBuildHeight(targetPos)) return null;
        if (!AutismContainerTarget.isWithinBlockReach(mc, targetPos)) return null;
        Direction face = bestFaceTowards(mc.player.getEyePosition(), targetPos);
        Vec3 center = Vec3.atCenterOf(targetPos);
        Vec3 hitPos = center.add(
            face.getStepX() * 0.5D,
            face.getStepY() * 0.5D,
            face.getStepZ() * 0.5D
        );
        return new BlockHitResult(hitPos, face, targetPos, false);
    }

    private static Direction bestFaceTowards(Vec3 eye, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        double dx = eye.x - center.x;
        double dy = eye.y - center.y;
        double dz = eye.z - center.z;
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ax >= ay && ax >= az) return dx > 0 ? Direction.WEST : Direction.EAST;
        if (ay >= ax && ay >= az) return dy > 0 ? Direction.DOWN : Direction.UP;
        return dz > 0 ? Direction.NORTH : Direction.SOUTH;
    }
}
