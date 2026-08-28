# alpha.5 implementation notes

This build keeps the global slowed world at 8 TPS while applying transient player-side gameplay attributes so the Reflex Drive user keeps approximately normal real-time movement, attack recovery and mining cadence relative to the slowed world. Ordinary item-use duration is shortened by the same compensation factor.

The Hollow Colossus uses a small non-pickable parent anchor and six pickable multipart regions. Client culling uses the union of the multipart boxes, and when vanilla entity hitbox display is enabled TITANBREAK draws the six actual part boxes in cyan because Minecraft 26.2 only renders multipart hitboxes specially for the Ender Dragon.
