module Api exposing (..)

import HttpBuilder exposing (..)
import Decoder
import Types exposing (..)
import Rocket exposing ((=>))
import Debug


getGroupUsers : GroupId -> Cmd Msg
getGroupUsers id =
    let
        toMsg result =
            case result of
                Err error ->
                    toString error
                        |> Debug.log
                        |> always NoOp

                Ok { data } ->
                    AddSelectedUsers data
    in
        String.join "/" [ "admin", "api", "group", toString id, "users" ]
            |> String.cons '/'
            |> HttpBuilder.get
            |> withHeaders [ "Accept" => "application/json" ]
            |> withCredentials
            |> attempt toMsg (jsonReader Decoder.users) stringReader


searchUsers : String -> Cmd Msg
searchUsers query =
    let
        toMsg result =
            case result of
                Err error ->
                    toString error
                        |> Debug.log
                        |> always NoOp

                Ok { data } ->
                    SetSearchedUsers data
    in
        String.join "/" [ "admin", "api", "user", "search" ++ "?q=" ++ query ]
            |> String.cons '/'
            |> HttpBuilder.get
            |> withHeaders [ "Accept" => "application/json" ]
            |> withCredentials
            |> attempt toMsg (jsonReader Decoder.users) stringReader
