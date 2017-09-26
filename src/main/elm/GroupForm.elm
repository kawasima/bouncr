module GroupForm exposing (..)

import Html exposing (Html, programWithFlags, button, div, text)
import Html.Events exposing (onClick)


main : Program Flags Model Msg
main =
    programWithFlags
        { init = init
        , view = view
        , update = update
        , subscriptions = subscriptions
        }


type alias Flags =
    { groupId : Maybe Int }



-- MODEL


type alias Model =
    { groupId : Maybe Int }


init : Flags -> ( Model, Cmd Msg )
init { groupId } =
    ( { groupId = groupId }, Cmd.none )



-- UPDATE


type Msg
    = NoOp


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        NoOp ->
            ( model, Cmd.none )



-- VIEW


view : Model -> Html Msg
view { groupId } =
    div [] [ text (toString groupId) ]


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.none
