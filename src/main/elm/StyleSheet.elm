module StyleSheet exposing (Styles(..), styleSheet, Element)

import Style exposing (..)
import Color exposing (Color)
import Element


type alias StyleSheet =
    Style.StyleSheet Styles ()


type alias Element msg =
    Element.Element Styles () msg


type Styles
    = None


styleSheet : StyleSheet
styleSheet =
    Style.styleSheet [ style None [] ]
