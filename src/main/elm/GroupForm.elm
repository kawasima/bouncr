module GroupForm exposing (..)

import Html exposing (Html, programWithFlags, button, div, text)
import Html.Events exposing (onClick)
import Types exposing (..)
import Api exposing (..)
import Rocket exposing (..)


main : Program Flags Model Msg
main =
    programWithFlags
        { init = init >> batchInit
        , view = view
        , update = update >> batchUpdate
        , subscriptions = subscriptions
        }



-- MODEL


init : Flags -> ( Model, List (Cmd Msg) )
init { groupId } =
    { groupId = groupId
    , selected = []
    }
        => []



-- UPDATE


update : Msg -> Model -> ( Model, List (Cmd Msg) )
update msg model =
    case msg of
        GetGroupUsers ->
            model => [ Api.getGroupUsers model ]

        SetGroupUsers users ->
            { model | selected = users }



-- VIEW


view : Model -> Html Msg
view { groupId } =
    div [] [ text (toString groupId) ]


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.none
