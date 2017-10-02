module GroupUsersSelecter exposing (..)

import Html exposing (Html, programWithFlags, button, div, text, input)
import Html.Events exposing (onClick, onInput)
import Html.Attributes as Attrs
import Time exposing (second)
import Debounce exposing (Debounce)
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
    , debounce = Debounce.init
    }
        => [ case groupId of
                Just id ->
                    Api.getGroupUsers id

                Nothing ->
                    Cmd.none
           ]


debounceConfig : Debounce.Config Msg
debounceConfig =
    { strategy = Debounce.later (0.5 * second)
    , transform = DebounceMsg
    }


debounceUpdate : Debounce.Msg -> Debounce String -> ( Debounce String, Cmd Msg )
debounceUpdate msg deboune =
    Debounce.update
        debounceConfig
        (Debounce.takeLast Api.searchUsers)
        msg
        deboune



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
            let
                ( debounce, cmd ) =
                    Debounce.push debounceConfig query model.debounce
            in
                { model | query = query, debounce = debounce } => [ cmd ]

        DebounceMsg msg ->
            let
                ( debounce, cmd ) =
                    debounceUpdate msg model.debounce
            in
                { model | debounce = debounce } => [ cmd ]



-- VIEW


view : Model -> Html Msg
view model =
    div []
        [ text (toString model)
        , input [ onInput SetQuery, Attrs.value model.query ] []
        ]


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.none
