module Types exposing (..)

import Debounce exposing (Debounce)
import Dict exposing (Dict)
import Http


type alias Flags =
    { groupId : Maybe Int }


type alias Model =
    { groupId : Maybe Id
    , users : Dict Id User
    , query : String
    , debounce : Debounce String
    }


type alias User =
    { id : Id
    , account : String
    , name : String
    , email : String
    , state : State
    }


type State
    = Searched
    | ReadySelected
    | Selected
    | ReadyTrashed
    | Retained


type alias Id =
    Int


type Msg
    = NoOp
    | FetchedGroupUsers (Result Http.Error (List User))
    | FetchedSearchedUsers (Result Http.Error (List User))
      -- | AddSelectedUser User
      -- | AddSelectedUsers (List User)
      -- | SetSearchedUsers (List User)
    | InputQuery String
    | DebounceMsg Debounce.Msg
    | CheckSearchedUser Id
    | UncheckSearchedUser Id
    | CheckSelectedUser Id
    | UncheckSelectedUser Id
    | SelectUsers
    | TrashUsers
