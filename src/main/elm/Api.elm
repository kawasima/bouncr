module Api exposing (..)

import HttpBuilder exposing (..)
import Decoder
import Types exposing (..)
import Rocket exposing ((=>))
import Debug


type Method
    = Get
    | Post
    | Put
    | Delete


request : Method -> List String -> List ( String, String ) -> RequestBuilder
request method paths params =
    let
        url_ =
            url paths params
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


getGroupUsers : GroupId -> Cmd Msg
getGroupUsers id =
    request Get [ "admin", "api", "group", toString id, "users" ] []
        |> withBase
        |> attempt (noOp AddSelectedUsers) (jsonReader Decoder.users) stringReader


searchUsers : String -> Cmd Msg
searchUsers query =
    request Get [ "admin", "api", "user", "search" ] [ "q" => query ]
        |> withBase
        |> attempt (noOp SetSearchedUsers) (jsonReader Decoder.users) stringReader


url : List String -> List ( String, String ) -> String
url paths params =
    let
        base =
            String.join "/" paths
                |> String.cons '/'
    in
        HttpBuilder.url base params


withBase : RequestBuilder -> RequestBuilder
withBase builder =
    builder
        |> withHeaders [ "Accept" => "application/json" ]
        |> withCredentials


noOp : (a -> Msg) -> Result (Error b) (Response a) -> Msg
noOp toMsg result =
    case result of
        Err error ->
            toString error
                |> Debug.log
                |> always NoOp

        Ok { data } ->
            toMsg data
