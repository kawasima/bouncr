module Types exposing (..)


type alias Flags =
    { groupId : Maybe Int }


type alias Model =
    { groupId : Maybe Int
    , selected : List User
    }


type alias User =
    { id : Int
    , account : String
    , name : String
    , email : String
    }


type Msg
    = GetGroupUsers
    | SetGroupUsers (List User)
