module GroupUsersSelector exposing (..)

import Html exposing (programWithFlags)
import Time exposing (second)
import Debounce exposing (Debounce)
import Types exposing (..)
import Dict exposing (Dict)
import View exposing (view)
import Api exposing (..)
import Rocket exposing (..)
import Debug exposing (crash, log)


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
    , users = Dict.empty
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
        (Debounce.takeLast
            (\query ->
                if String.isEmpty query then
                    Cmd.none
                else
                    Api.searchUsers query
            )
        )
        msg
        deboune



-- UPDATE


update : Msg -> Model -> ( Model, List (Cmd Msg) )
update msg model =
    case msg of
        NoOp ->
            model => []

        FetchedGroupUsers (Err err) ->
            log "FetchedGroupUsers Err" err
                |> always model
                => []

        FetchedGroupUsers (Ok users) ->
            { model | users = insertUsers users model.users }
                => []

        FetchedSearchedUsers (Err err) ->
            log "FetchedSearchedUsers Err" err
                |> always model
                => []

        FetchedSearchedUsers (Ok users) ->
            { model | users = insertUsers users model.users }
                => []

        InputQuery query ->
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


{-| Insert users for fetched groupUsers and fetched searchedUsers
-}
insertUsers : List User -> Dict Id User -> Dict Id User
insertUsers users dict =
    List.foldl (\user dict -> insertUser user dict) dict users


insertUser : User -> Dict Id User -> Dict Id User
insertUser new users =
    let
        replace : User -> User
        replace old =
            case ( old.state, new.state ) of
                ( Trashed, Searched ) ->
                    new

                ( Searched, Searched ) ->
                    new

                ( ReadySelected, Searched ) ->
                    new

                ( Selected, Searched ) ->
                    { new | state = Selected }

                ( _, Selected ) ->
                    new

                _ ->
                    crash "TODO : These branches is not in use."
    in
        Dict.update new.id
            (\maybe ->
                case maybe of
                    Nothing ->
                        Just new

                    Just user ->
                        replace user |> Just
            )
            users


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.none
