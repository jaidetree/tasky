module Document = {
  @val @scope("document") @return(nullable)
  external getElementById: string => option<Dom.element> = "getElementById"
}

@val external document: Dom.document = "document"

module Window = {
  type t = Dom.element
}

@val external window: Window.t = "window"

module FormData = {
  type t
  @new external make: Dom.element => t = "FormData"

  @send @returns(nullable) external get: (t, string) => option<string> = "get"
}

@get @returns(nullable) external getName: Dom.element => option<string> = "name"
