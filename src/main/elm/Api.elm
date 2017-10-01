module Api exposing (..)

import Http exposing (Error(..), Response, expectJson, expectString, expectStringResponse)
import HttpBuilder exposing (..)
import Json.Decode exposing (int, string, float, list, Decoder)
import Json.Decode.Pipeline exposing (decode, required, optional, hardcoded)
import Types exposing (..)
import Rocket exposing ((=>))


getGroupUsers : Model -> Cmd Msg
getGroupUsers model =
    -- Task.succeed []
    Cmd.none
