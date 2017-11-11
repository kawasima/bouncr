module View exposing (view)

import Types exposing (..)
import Dict
import StyleSheet exposing (Styles(..), Element, styleSheet, Attribute)
import Html exposing (Html)
import Html.Attributes
import Element exposing (column, row, text, node, el, empty, decorativeImage)
import Element.Attributes as Attrs exposing (..)
import Element.Events exposing (onInput, on, targetValue, onClick)
import Element.Input as Input exposing (labelAbove)
import Gravatar
import Debug exposing (crash, log)


view : Model -> Html Msg
view model =
    root model
        |> Element.layout styleSheet


root : Model -> Element Msg
root model =
    row None
        []
        [ userSearch model
        , panel model
        , selectedUsers model
        ]


panel : Model -> Element Msg
panel { users } =
    column None
        []
        [ icon
            (if Dict.values users |> List.any (\user -> user.state == ReadySelected) then
                IconChecked
             else
                IconUnchecked
            )
            [ verticalCenter
            , onClick
                (if Dict.values users |> List.any (\user -> user.state == ReadySelected) then
                    SelectUsers
                 else
                    NoOp
                )
            ]
            "fa-users fa-2x"
        , icon
            (if Dict.values users |> List.any (\user -> user.state == ReadyTrashed) then
                IconChecked
             else
                IconUnchecked
            )
            [ verticalCenter
            , onClick
                (if Dict.values users |> List.any (\user -> user.state == ReadyTrashed) then
                    TrashUsers
                 else
                    NoOp
                )
            ]
            "fa-trash-o fa-2x"
        ]


userSearch : Model -> Element Msg
userSearch model =
    column None
        []
        [ Input.text None
            []
            { onChange = InputQuery
            , value = model.query
            , label = labelAbove <| text "User Search"
            , options = []
            }
        , column None
            []
          <|
            List.map searchedUser <|
                Dict.values <|
                    Dict.filter
                        (\_ user ->
                            user.state == Searched || user.state == ReadySelected
                        )
                        model.users
        ]


searchedUser : User -> Element Msg
searchedUser data =
    row None
        [ spacing 5
        , case data.state of
            ReadySelected ->
                onClick <| UncheckSearchedUser data.id

            Searched ->
                onClick <| CheckSearchedUser data.id

            _ ->
                crash "This branch is not used."
        ]
        [ icon
            (case data.state of
                ReadySelected ->
                    IconChecked

                Searched ->
                    IconUnchecked

                _ ->
                    crash "This branch is not used."
            )
            [ verticalCenter ]
            "fa-users fa-2x"
        , user data
        ]


selectedUsers : Model -> Element Msg
selectedUsers model =
    column None
        []
        [ column None
            []
          <|
            List.map selectedUser <|
                Dict.values <|
                    Dict.filter
                        (\_ user ->
                            user.state == Selected || user.state == ReadyTrashed
                        )
                        model.users
        ]


selectedUser : User -> Element Msg
selectedUser data =
    row None
        [ spacing 5
        , case data.state of
            Selected ->
                onClick <| CheckSelectedUser data.id

            ReadyTrashed ->
                onClick <| UncheckSelectedUser data.id

            _ ->
                crash "This branch is not used."
        ]
        [ icon
            (case data.state of
                ReadyTrashed ->
                    IconChecked

                Selected ->
                    IconUnchecked

                _ ->
                    crash "This branch is not used."
            )
            [ verticalCenter ]
            "fa-trash-o fa-2x"
        , user data
        ]


user : User -> Element msg
user { account, name, email, id } =
    row None
        [ spacing 5 ]
        [ gravatar 32 email
        , column None
            []
            [ row None [ spacing 5 ] [ text name ]
            , row None
                [ spacing 5 ]
                [ icon None [ verticalCenter ] "fa-id-card-o"
                , text account
                ]
            , row None
                [ spacing 5 ]
                [ icon None [ verticalCenter ] "fa-envelope-o"
                , text email
                ]
            ]
        ]


icon : Styles -> List (Attribute msg) -> String -> Element msg
icon style attrs class =
    Html.i [ Html.Attributes.class <| "fa " ++ class ++ " fa-fw" ] []
        |> Element.html
        |> el style attrs


gravatar : Int -> String -> Element msg
gravatar size email =
    decorativeImage None
        [ width <| px <| toFloat size
        , height <| px <| toFloat size
        , verticalCenter
        ]
        { src =
            Gravatar.url
                (Gravatar.defaultOptions |> Gravatar.withSize (Just size))
                email
        }
