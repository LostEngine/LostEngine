---@param player Player
---@param blockHit BlockHit
function onClick(player, blockHit)
    if blockHit ~= nil then
        local x, y, z = blockHit.getPosition()
        player.sendMessage("You clicked at " .. x .. "," .. y .. "," .. z)
        player.getWorld().spawnEntity("minecraft:lightning_bolt", x, y, z)
    end
end
