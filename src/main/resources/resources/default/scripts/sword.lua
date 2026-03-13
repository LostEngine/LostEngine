function onClick(player, x, y, z)
    player.sendMessage("You clicked at " .. x .. "," .. y .. "," .. z)
    player.getWorld().spawnEntity("minecraft:lightning_bolt", x, y, z)
end
