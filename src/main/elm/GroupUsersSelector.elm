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
            { model
                | users =
                    model.users
                        |> Dict.map (\_ user -> transitState [ Searched => Trashed ] user)
                        |> insertUsers users
            }
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

        CheckSearchedUser id ->
            { model
                | users =
                    model.users
                        |> Dict.update id (Maybe.map (transitState [ Searched => ReadySelected ]))
            }
                => []

        UncheckSearchedUser id ->
            { model
                | users =
                    model.users
                        |> Dict.update id (Maybe.map (transitState [ ReadySelected => Searched ]))
            }
                => []

        CheckSelectedUser id ->
            { model
                | users =
                    model.users
                        |> Dict.update id (Maybe.map (transitState [ Selected => ReadyTrashed ]))
            }
                => []

        UncheckSelectedUser id ->
            { model
                | users =
                    model.users
                        |> Dict.update id (Maybe.map (transitState [ ReadyTrashed => Selected ]))
            }
                => []

        SelectUsers ->
            { model
                | users =
                    model.users
                        |> Dict.map (\id user -> transitState [ ReadySelected => Selected ] user)
            }
                => []

        TrashUsers ->
            { model
                | users =
                    model.users
                        |> Dict.map (\id user -> transitState [ ReadyTrashed => Trashed ] user)
            }
                => []


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
            case old.state of
                Trashed ->
                    new

                Searched ->
                    new

                ReadySelected ->
                    { new | state = ReadySelected }

                Selected ->
                    { new | state = Selected }

                ReadyTrashed ->
                    { new | state = ReadyTrashed }
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


transitState : List ( State, State ) -> User -> User
transitState transitions user =
    List.foldl
        (\( before, after ) user ->
            if before == user.state then
                { user | state = after }
            else
                user
        )
        user
        transitions


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.none
