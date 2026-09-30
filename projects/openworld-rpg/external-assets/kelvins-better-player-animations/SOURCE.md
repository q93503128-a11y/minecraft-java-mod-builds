# Kelvin's Better Player Animations — Dodge/Roll editable base

Production use: Openworld RPG player dodge/roll presentation.

## Source

- upstream repository: `Kelvin285/KelvinsBetterPlayerAnimations`
- pinned commit: `e0dda1ff2874756490d3b422db524ea67fad3448`
- source file: `src/main/resources/assets/betteranimations/player_animation/rolling.json`
- source Git blob: `404f547cf00434cf5d538ee5b9de25bb11e6ca1b`
- upstream animation length: 0.375 s
- license: MIT
- copyright: Copyright (c) 2023 Kevin Merrill

Animation Overhaul independently carries the same `rolling.json` and explicitly states that its
`src/main/resources/assets/animation_overhaul/player_animation` directory is MIT-licensed, while
crediting Kelvin285/Kevin Merrill as the animation source. The direct Kelvin repository and its MIT
LICENSE are the primary provenance used here.

## Project adaptation

Project file:

- `assets/openworld_rpg/player_animations/dodge_roll.json`

The project treats the source as an **editable base**, not an unmodified asset:

- animation time is scaled by exactly 1.2 so 0.375 s becomes the canonical 0.45 s / 9-tick dodge action;
- four named PAL animations are emitted: `dodge_forward`, `dodge_backward`, `dodge_left`, `dodge_right`;
- backward/left/right variants add a whole-body local yaw offset so the visible roll direction follows the server-authoritative movement direction;
- gameplay distance, i-frames, Stamina cost, re-entry, collision and acceptance remain project server authority and are not derived from animation curves.

Final Minecraft clipping, armor compatibility, first/third-person camera feel and multiplayer presentation still require playtest.

## MIT License

Copyright (c) 2023 Kevin Merrill

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial
portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
