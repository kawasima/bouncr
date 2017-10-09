module View exposing (view)

import Types exposing (..)
import StyleSheet exposing (Styles(..), Element, styleSheet)
import Html exposing (Html)
import Element exposing (..)
import Element.Attributes as Attrs exposing (..)
import Element.Events exposing (onInput, on, targetValue, onClick)


view : Model -> Html Msg
view model =
    rootElement model
        |> Element.layout styleSheet


rootElement : Model -> StyleSheet.Element Msg
rootElement model =
    column None
        []
        []
