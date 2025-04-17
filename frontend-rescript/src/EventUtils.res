module AbortController = {
  module Signal = {
    type t = {
      aborted: bool,
      reason: option<string>,
    }

    @send
    external throwIfAborted: t => unit = "throwIfAborted"

    @scope("AbortSignal") @val
    external abort: unit => t = "abort"

    @scope("AbortSignal") @val
    external any: array<t> => t = "any"

    @scope("AbortSignal") @val
    external timeout: int => t = "timeout"
  }

  type t = {signal: Signal.t}

  @new @module
  external make: unit => t = "AbortController"

  @send
  external abort: t => unit = "abort"

  @get
  external signal: t => Signal.t = "signal"
}

type addListenerOptions = {
  capture?: bool,
  once?: bool,
  passive?: bool,
  signal?: AbortController.Signal.t,
}

type listener<'a> = Dom.event_like<'a> => unit

@send
external addEventListener: (
  Dom.element,
  string,
  listener<'a>,
  ~opts: addListenerOptions=?,
) => unit = "addEventListener"

type removeListenerOptions = {capture?: bool}

@send
external removeEventListener: (
  Dom.element,
  string,
  listener<'a>,
  ~opts: removeListenerOptions=?,
) => unit = "removeEventListener"

@get @return(nullable)
external getValue: Dom.element => option<string> = "value"

// module MouseEvent = {
//   type t = Dom.mouseEvent
//   @get external pageX: t => int = "pageX"
//   @get external pageY: t => int = "pageY"
//
//   @get
//   external target: t => Dom.eventTarget = "target"
//
//   @get
//   external currentTarget: t => Dom.eventTarget = "currentTarget"
// }

module FormEvent = {
  type t = JsxEvent.Form.t

  @get @return(nullable) external currentTarget: t => option<Dom.element> = "currentTarget"
  @get external target: t => Dom.element = "target"
}
