module Types exposing (..)

import Debounce exposing (Debounce)


type alias Flags =
    { groupId : Maybe Int }


type alias Model =
    { groupId : Maybe GroupId
    , selected : List User
    , searched : List User
    , query : String
    , debounce : Debounce String
    }


type alias User =
    { id : Int
    , account : String
    , name : String
    , email : String
    }


type alias GroupId =
    Int


type Msg
    = NoOp
    | FetchGroupUsers GroupId
    | AddSelectedUser User
    | AddSelectedUsers (List User)
    | SetSearchedUsers (List User)
    | SearchUsers
    | SetQuery String
    | DebounceMsg Debounce.Msg
