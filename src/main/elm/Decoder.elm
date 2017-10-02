module Decoder exposing (..)

import Json.Decode exposing (int, string, float, list, Decoder)
import Json.Decode.Pipeline exposing (decode, required, optional, hardcoded)
import Types exposing (..)


users : Decoder (List User)
users =
    decode User
        |> required "id" int
        |> required "account" string
        |> required "name" string
        |> required "email" string
        |> list
