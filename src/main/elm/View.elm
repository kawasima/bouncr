module View exposing (view)

import Types exposing (..)
import StyleSheet exposing (Styles(..), Element, styleSheet)
import Html exposing (Html)
import Element exposing (column, row, text)
import Element.Attributes as Attrs exposing (..)
import Element.Events exposing (onInput, on, targetValue, onClick)
import Element.Input as Input exposing (labelAbove)


view : Model -> Html Msg
view model =
    root model
        |> Element.layout styleSheet


root : Model -> Element Msg
root model =
    row None
        []
        [ userSearch model
        ]


userSearch : Model -> Element Msg
userSearch model =
    column None
        []
        [ Input.text None
            []
            { onChange = SetQuery
            , value = model.query
            , label = labelAbove <| text "User Search"
            , options = []
            }
        , column None
            []
          <|
            List.map searchedUser model.searched
        ]


searchedUser : User -> Element Msg
searchedUser { account, name, email, id } =
    row None
        [ spacing 5 ]
        [ text "icon"
        , column None
            []
            [ row None [ spacing 5 ] [ text "account", text account ]
            , row None [ spacing 5 ] [ text "name", text name ]
            , row None [ spacing 5 ] [ text "email", text email ]
            ]
        ]
