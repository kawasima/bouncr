module Dev exposing (..)

import GroupUsersSelecter exposing (..)
import Html exposing (program)
import Types exposing (..)
import Rocket exposing (..)
import View exposing (view)


main : Program Never Model Msg
main =
    program
        { init = init { groupId = Just 2 } |> batchInit
        , view = view
        , update = update >> batchUpdate
        , subscriptions = subscriptions
        }
