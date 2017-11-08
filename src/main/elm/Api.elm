module Api exposing (..)

import Http
import HttpBuilder exposing (..)
import Json.Decode
import Decoder
import Types exposing (..)
import Rocket exposing ((=>))
import Debug


type Method
    = Get
    | Post
    | Put
    | Delete


request : Method -> List String -> RequestBuilder ()
request method paths =
    let
        url_ =
            url paths
    in
        case method of
            Get ->
                HttpBuilder.get url_

            Post ->
                HttpBuilder.post url_

            Put ->
                HttpBuilder.put url_

            Delete ->
                HttpBuilder.delete url_


getGroupUsers : Id -> Cmd Msg
getGroupUsers id =
    request Get [ "admin", "api", "group", toString id, "users" ]
        |> withBase
        |> withDecoder (Decoder.users Selected)
        |> send FetchedGroupUsers


searchUsers : String -> Cmd Msg
searchUsers query =
    request Get [ "admin", "api", "user", "search" ]
        |> withBase
        |> withQueryParams [ "q" => query ]
        |> withDecoder (Decoder.users Searched)
        |> send FetchedSearchedUsers


url : List String -> String
url paths =
    String.join "/" paths
        |> String.cons '/'


withBase : RequestBuilder a -> RequestBuilder a
withBase builder =
    builder
        |> withHeaders [ "Accept" => "application/json" ]
        |> withCredentials


withDecoder : Json.Decode.Decoder a -> RequestBuilder igonore -> RequestBuilder a
withDecoder decoder builder =
    builder
        |> withExpect (Http.expectJson decoder)
