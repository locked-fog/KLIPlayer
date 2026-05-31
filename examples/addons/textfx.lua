return {
  id = "textfx",
  version = "0.1.0",
  functions = {
    type = function(ctx)
      local text = ctx.string("text")
      local interval = ctx.duration("interval", "80ms")
      local events = {}

      for i, ch in ipairs(ctx.chars(text)) do
        events[#events + 1] = {
          offset = (i - 1) * interval,
          ops = {
            { op = "text", value = ch }
          }
        }
      end

      return events
    end,

    flash = function(ctx)
      local color = ctx.color("color", "ff0055")
      local text = ctx.string("text", "!")

      return {
        {
          offset = 0,
          z = 80,
          ops = {
            { op = "color", value = color },
            { op = "text", value = text }
          }
        },
        {
          offset = ctx.duration("hold", "120ms"),
          z = 80,
          ops = {
            { op = "color", value = "default" },
            { op = "cleanline" }
          }
        }
      }
    end
  }
}
