type requestInit<'a> = 'a
type response

@val external fetch: (string, requestInit<'a>) => promise<response> = "fetch"

@send external json: response => promise<Js.Json.t> = "json"
