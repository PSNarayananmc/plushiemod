// scripts/plushie-party/main.ts
import { system as system2, world as world2 } from "@minecraft/server";

// data/scripts/PlushieParty.ts
import {
  EquipmentSlot,
  system,
  world
} from "@minecraft/server";
var PLUSHIE_PREFIX = "re_furn:plushies_";
var MUSIC_DISC_PREFIX = "minecraft:music_disc_";
var BAR_TICKS = 74;
var PARTY_RADIUS = 16;
var DANCE_ANIMATION = "animation.re_furn.party_dance";
var PLAYER_DANCE_ANIMATION = "animation.re_furn.party_dance_player";
var STOP_ANIMATION = "animation.re_furn.party_stop";
var DANCE_OPTIONS = { blendOutTime: 0.25, stopExpression: "0" };
var STOP_OPTIONS = { blendOutTime: 0.25 };
var parties = new Map;
var dancers = new Map;
var barRunId;
function syncDancers(startNewcomers) {
  const guests = new Set;
  for (const plushie of parties.values()) {
    if (!plushie.isValid) {
      parties.delete(plushie.id);
      continue;
    }
    const options = { location: plushie.location, maxDistance: PARTY_RADIUS };
    const nearby = plushie.dimension.getEntities({ ...options, families: ["mob"] });
    for (const player of plushie.dimension.getPlayers(options))
      nearby.push(player);
    nearby.push(plushie);
    for (const guest of nearby) {
      guests.add(guest.id);
      if (!startNewcomers || dancers.has(guest.id))
        continue;
      dancers.set(guest.id, guest);
      guest.playAnimation(guest.typeId === "minecraft:player" ? PLAYER_DANCE_ANIMATION : DANCE_ANIMATION, DANCE_OPTIONS);
    }
  }
  for (const id of dancers.keys()) {
    if (guests.has(id))
      continue;
    const dancer = dancers.get(id);
    dancers.delete(id);
    if (dancer?.isValid)
      dancer.playAnimation(STOP_ANIMATION, STOP_OPTIONS);
  }
  if (parties.size > 0 || barRunId === undefined)
    return;
  system.clearRun(barRunId);
  barRunId = undefined;
}
function handlePlushiePartyInteract(player, plushie) {
  if (!plushie.typeId.startsWith(PLUSHIE_PREFIX))
    return;
  toggleParty(player, plushie);
}
function toggleParty(player, host) {
  if (!player.isValid || !host.isValid)
    return;
  if (parties.has(host.id)) {
    parties.delete(host.id);
    syncDancers(false);
    return;
  }
  const item = player.getComponent("minecraft:equippable")?.getEquipment(EquipmentSlot.Mainhand);
  if (!item)
    return;
  if (item.typeId !== "minecraft:stick" && !item.typeId.startsWith(MUSIC_DISC_PREFIX))
    return;
  parties.set(host.id, host);
  if (barRunId === undefined)
    barRunId = system.runInterval(() => syncDancers(true), BAR_TICKS);
  syncDancers(true);
}
world.afterEvents.entityRemove.subscribe(({ removedEntityId }) => {
  parties.delete(removedEntityId);
  dancers.delete(removedEntityId);
});

// scripts/plushie-party/main.ts
var FURNITURE_HITBOX_PATTERN = /^re_furn:plushies_(.+)_hitbox$/;
var CREEPER_BLOCK = "re_plushie_party:creeper_plushie_hitbox";
var CREEPER_ENTITY = "re_plushie_party:creeper_plushie";
var PLACED_STATE = "re_plushie_party:placed";
var YAW_BY_DIRECTION = { south: 180, north: 0, east: 90, west: 270 };
function findPlushie(block, type) {
  const nearby = block.dimension.getEntities({ type, location: block.center(), maxDistance: 1 });
  for (const plushie of nearby) {
    const { x, y, z } = plushie.location;
    if (Math.floor(x) === block.x && Math.floor(y) === block.y && Math.floor(z) === block.z)
      return plushie;
  }
  return;
}
world2.beforeEvents.playerInteractWithBlock.subscribe((event) => {
  if (!event.isFirstEvent)
    return;
  const { block, player } = event;
  const typeId = block.typeId;
  const match = FURNITURE_HITBOX_PATTERN.exec(typeId);
  if (!match && typeId !== CREEPER_BLOCK)
    return;
  system2.run(() => {
    if (!block.isValid)
      return;
    if (match) {
      const plushie2 = findPlushie(block, `re_furn:plushies_${match[1]}`);
      if (plushie2)
        handlePlushiePartyInteract(player, plushie2);
      return;
    }
    const plushie = findPlushie(block, CREEPER_ENTITY);
    if (!plushie)
      return;
    plushie.triggerEvent("re_event:variant0");
    toggleParty(player, plushie);
  });
});
world2.afterEvents.playerPlaceBlock.subscribe(({ block }) => {
  if (block.typeId !== CREEPER_BLOCK)
    return;
  const direction = block.permutation.getState("minecraft:cardinal_direction");
  block.setPermutation(block.permutation.withState(PLACED_STATE, 1));
  block.dimension.spawnEntity(CREEPER_ENTITY, { x: block.x + 0.5, y: block.y, z: block.z + 0.5 }, { initialRotation: YAW_BY_DIRECTION[String(direction)] ?? 180 });
});
world2.afterEvents.playerBreakBlock.subscribe(({ block, brokenBlockPermutation }) => {
  if (brokenBlockPermutation.type.id !== CREEPER_BLOCK)
    return;
  findPlushie(block, CREEPER_ENTITY)?.remove();
});
