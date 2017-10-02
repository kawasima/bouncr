module GroupForm exposing (..)

import Html exposing (Html, programWithFlags, button, div, text, input)
import Html.Events exposing (onClick, onInput)
import Html.Attributes as Attrs
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
    , searched = []
    , query = ""
    }
        => [ case groupId of
                Just id ->
                    Api.getGroupUsers id

                Nothing ->
                    Cmd.none
           ]



-- UPDATE


update : Msg -> Model -> ( Model, List (Cmd Msg) )
update msg model =
    case msg of
        NoOp ->
            model => []

        FetchGroupUsers id ->
            model => [ Api.getGroupUsers id ]

        AddSelectedUser user ->
            { model | selected = user :: model.selected } => []

        AddSelectedUsers users ->
            { model | selected = users ++ model.selected } => []

        SetSearchedUsers users ->
            { model | searched = users } => []

        SearchUsers ->
            model => [ Api.searchUsers model.query ]

        SetQuery query ->
            { model | query = query } => [ Api.searchUsers query ]



-- VIEW


view : Model -> Html Msg
view model =
    div []
        [ text (toString model)
        , input [ onInput SetQuery, Attrs.value model.query ] []
        , button [ onClick SearchUsers ] [ text "Incremental Searchだよ！" ]
        ]


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.none
