export const luaAPI =
`---@class World
local World = {}

---Sets a block at the given coordinates.
---@param block string The block identifier
---@param x number
---@param y number
---@param z number
function World:setBlock(block, x, y, z) end

---Spawns an entity at the given coordinates.
---@param entity string The entity identifier
---@param x number
---@param y number
---@param z number
function World:spawnEntity(entity, x, y, z) end

---@class Player
local Player = {}

---Returns player's position and rotation
---@return number x
---@return number y
---@return number z
---@return number yaw
---@return number pitch
function Player:getPosition() end

---Gives an item to the player
---@param item string
---@param count number
function Player:giveItem(item, count) end

---Sends a chat message to the player
---@param msg string
function Player:sendMessage(msg) end

---Executes a command as the player
---@param cmd string
function Player:execute(cmd) end

---Returns the world the player is in
---@return World
function Player:getWorld() end`;