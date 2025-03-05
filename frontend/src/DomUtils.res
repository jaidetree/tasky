module Document = {
  @val @scope("document") @return(nullable)
  external getElementById: string => option<Dom.element> = "getElementById"
}

@val external document: Dom.document = "document"

module Window = {
  type t = Dom.element
}

@val external window: Window.t = "window"
