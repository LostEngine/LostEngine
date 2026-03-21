// noinspection all

export const luaApiGlobals = `

_VERSION = Luaj-jse 3.0.1

---@generic T, R
---@param f sync fun(...:T...): R...
---@param msgh fun(err:any):any
---@param ... T...
---@return boolean, R...
function xpcall(f, msgh, ...) end

---@param v any
---@return string
function tostring(v) end

function print(...) end

---@generic K, V, I
---@param t table<K, V> | V[] | {[K]: V}
---@return (fun(tbl: table<I, V>, index: I?):K, V), table<I, V>, I?
function pairs(t) end

---@generic T: table
---@param table T
---@param metatable any|table|nil
---@return T
function setmetatable(table, metatable) end

---@generic K, V
---@overload fun(table:table<K, V>):K?,V?
---@param table table<K, V> | V[] | {[K]: V}
---@param index? K
---@return K?, V?
function next(table, index) end

---@generic T, T1
---@param v T
---@param ... T1...
---@return T, T1...
function assert(v, ...) end

---@param v string|table
---@return integer
function rawlen(v) end

---@generic V
---@param t V[] | table<int, V> | {[int]: V}
---@return fun(tbl: any):int, V
function ipairs(t) end

---@overload fun(e: string, base: integer):integer?
---@param e any
---@return number?
---@nodiscard
function tonumber(e) end

---@param v1 any
---@param v2 any
---@return boolean
function rawequal(v1, v2) end

---@param object any
---@return any
function getmetatable(object) end

---@param table table
---@param index any
---@param value any
---@return table
function rawset(table, index, value) end

---@generic T, R
---@param f sync fun(...: T...): R...
---@param ... T...
---@return_overload true, R...
---@return_overload false, string
function pcall(f, ...) end

---@param v any
---@return any type
function type(v) end

---@generic T, Num: integer | '#'
---@param index any
---@param ... T...
---@return any
function select(index, ...) end

---@generic T, K
---@param table T
---@param index any
---@return any
function rawget(table, index) end

---@param message any
---@param level? integer
function error(message, level) end

---@class string
local string = {}

---@param s  string|number
---@param i  integer
---@param j? integer
---@return string
---@nodiscard
function string.sub(s, i, j) end

---@param s       string|number
---@param pattern string|number
---@param init?   integer
---@param plain?  boolean
---@return integer? start
---@return integer? end
---@return string?... captured
---@nodiscard
function string.find(s, pattern, init, plain) end

---@param s string
---@param n integer
---@param sep? string
---@return string
function string.rep(s, n, sep) end

---@param s string
---@param pattern string
---@param init? integer
---@return string?...
function string.match(s, pattern, init) end

---@param s string
---@param pattern string
---@return fun():string?...
function string.gmatch(s, pattern) end

---@param ... integer
---@return string
function string.char(...) end

---@param func function
---@param strip? boolean
---@return string
function string.dump(func, strip) end

---@param s string
---@return string
function string.reverse(s) end

---@param s string
---@return string
function string.upper(s) end

---@param s string
---@return integer
function string.len(s) end

---@param s       string|number
---@param pattern string|number
---@param repl string|number|table|fun(param:string)
---@param n? integer
---@return string
---@return integer count
function string.gsub(s, pattern, repl, n) end

---@param s string
---@param i? integer
---@param j? integer
---@return integer
function string.byte(s, i, j) end

---@param fmt string
---@param ... any
---@return string
---@nodiscard
function string.format(fmt, ...) end

---@param s string
---@return string
function string.lower(s) end

---@class table
local table = {}

---@generic V
---@param list table<integer, V> | V[]
---@param pos? integer
---@return V
function table.remove(list, pos) end

---@generic T
---@param ... T...
---@return [T...] | { n: integer }
---@nodiscard
function table.pack(...) end

---@param list table
---@param sep? string
---@param i?   integer
---@param j?   integer
---@return string
---@nodiscard
function table.concat(list, sep, i, j) end

---@generic V
---@param list V[]
---@param comp? fun(a:V, b:V):boolean
function table.sort(list, comp) end

---@overload fun(list:table, value:any)
---@param list table
---@param pos integer
---@param value any
function table.insert(list, pos, value) end

---@generic T, Start: integer, End: integer
---@param i? any
---@param j? any
---@param list T
---@return any
function table.unpack(list, i, j) end

---@class bit32
local bit32 = {}

---@return integer
---@nodiscard
function bit32.band(...) end

---@param x integer
---@param distp integer
---@return integer
---@nodiscard
function bit32.lrotate(x, distp) end

---@param n integer
---@param field integer
---@param width? integer
---@return integer
---@nodiscard
function bit32.extract(n, field, width) end

---@param x integer
---@param distp integer
---@return integer
---@nodiscard
function bit32.rshift(x, distp) end

---@return integer
---@nodiscard
function bit32.bor(...) end

---@param x integer
---@return integer
---@nodiscard
function bit32.bnot(x) end

---@param x integer
---@param disp integer
---@return integer
---@nodiscard
function bit32.arshift(x, disp) end

---@return integer
---@nodiscard
function bit32.bxor(...) end

---@param n integer
---@param v integer
---@param field integer
---@param width? integer
---@nodiscard
function bit32.replace(n, v, field, width) end

---@param x integer
---@param distp integer
---@return integer
---@nodiscard
function bit32.lshift(x, distp) end

---@return boolean
---@nodiscard
function bit32.btest(...) end

---@param x integer
---@param distp integer
---@return integer
---@nodiscard
function bit32.rrotate(x, distp) end

---@class server
local server = {}

---Executes a command in the console
---@param cmd string
function server.execute(cmd) end

---Sends a message to every player and the console (supports the minimessage format)
---@param msg string
function server.broadcast(msg) end

---Returns the time in milliseconds (see java.lang.System#currentTimeMillis)
---@return number
function server.time() end

---@class math
local math = {}

---@param x number
---@param base? number
---@return number
function math.log(x, base) end

---@param x number
---@return number
function math.exp(x) end

---@param x number
---@return number
function math.acos(x) end

---@param y number
---@param x? number
---@return number
function math.atan(y, x) end

---@param m number
---@param e integer
---@return number
function math.ldexp(m, e) end

---@param x number
---@return number
function math.deg(x) end

---@param x number
---@return number
function math.rad(x) end

---@param x number
---@return number
function math.tan(x) return 0 end

---@param x number
---@return number
function math.cos(x) end

---@param x number
---@return number
function math.cosh(x) end

---@overload fun():number
---@overload fun(m:integer):integer
---@param m integer
---@param n integer
---@return integer
function math.random(m, n) end

---@param x number
---@return number, integer
function math.frexp(x) end

---@param x integer
function math.randomseed(x) end

---@param x number
---@return integer
function math.ceil(x) end

---type number
math.pi = 3.1415927

---@param x number
---@return number
function math.tanh(x) end

---@param x number
---@return integer
function math.floor(x) end

---@overload fun(x:integer):integer
---@param x number
---@return number
function math.abs(x) end

---@overload fun(x:integer, ...:integer):integer
---@param x number
---@param ... number
---@return number
function math.max(x, ...) end

---@param x number
---@return number
function math.sqrt(x) return 0 end

---@param x number
---@return integer
---@return number
function math.modf(x) end

---@param x number
---@return number
function math.sinh(x) end

---type number
math.huge = inf

---@param x number
---@return number
function math.asin(x) end

---@overload fun(x:integer, ...:integer):integer
---@param x number
---@param ... number
---@return number
function math.min(x, ...) end

---@param x number
---@param y number
---@return number
function math.fmod(x, y) end

---@version 5.1, 5.2, JIT
---@param x number The base
---@param y number The exponent
---@return number
function math.pow(x, y) end

---@param y number
---@param x number
---@return number
function math.atan2(y, x) end

---@param x number
---@return number
function math.sin(x) return 0 end

---@class coroutine
local coroutine = {}

---@return thread?
---@nodiscard
function coroutine.running() end

---@param co thread
---@param val1? any
---@param ... any
---@return boolean success
---@return any ...
function coroutine.resume(co, val1, ...) end

---@async
---@param ... any
---@return any ...
function coroutine.yield(...) end

---@param co thread
---@return
---@nodiscard
function coroutine.status(co) end

---@param f async fun(...):any...
---@return fun(...):any...
---@nodiscard
function coroutine.wrap(f) end

---@param f async fun(...):any...
---@return thread
---@nodiscard
function coroutine.create(f) end
`;

export const luaApiClasses = `
---@class World
local World = {}

---Sets a block at the given coordinates.
---@param block string The block identifier
---@param x number
---@param y number
---@param z number
function World.setBlock(block, x, y, z) end

---Spawns an entity at the given coordinates.
---@param entity string The entity identifier
---@param x number
---@param y number
---@param z number
function World.spawnEntity(entity, x, y, z) end

---@class Player
local Player = {}

---Returns player's position and rotation
---@return number x
---@return number y
---@return number z
---@return number yaw
---@return number pitch
function Player.getPosition() end

---Gives an item to the player
---@param item string
---@param count number
function Player.giveItem(item, count) end

---Sends a chat message to the player
---@param msg string
function Player.sendMessage(msg) end

---Sends an action bar message to the player
---@param msg string
function Player.sendActionBar(msg) end

---Toggles the ability of the player to fly
---@param fly bool
function Player.toggleFly(fly) end

---Returns whether the player is able to fly or not
---@return bool
function Player.canFly() end

---Feeds the player
---@param nutrition number
---@param saturationLevel number
function Player.feed(nutrition, saturationLevel) end

---Heal the player
---@param amount number
function Player.heal(amount) end

---Hurts the player
---@param amount number
function Player.damage(amount) end

---Executes a command as the player
---@param cmd string
function Player.execute(cmd) end

---Returns the world the player is in
---@return World
function Player.getWorld() end

---@class BlockHit
local BlockHit = {}

---Returns the location where the player clicked
---@return number x (double)
---@return number y (double)
---@return number z (double)
function BlockHit.getLocation() end

---Returns the position of the block clicked by the player
---@return number x (int)
---@return number y (int)
---@return number z (int)
function BlockHit.getBlockPos() end

---@alias Direction
---| "down"
---| "up"
---| "north"
---| "south"
---| "west"
---| "east"

---Returns the side of the block clicked by the player
---@return string Direction 
function BlockHit.getDirection() end
`;

export const luaApi = luaApiGlobals + luaApiClasses + "\n";

export const luaApiLines = (luaApi.match(/\n/g) || '').length + 1;
