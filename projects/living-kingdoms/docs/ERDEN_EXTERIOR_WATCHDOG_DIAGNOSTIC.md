# Erden Exterior v2 Watchdog Diagnostic

A repeatable fresh-world CI watchdog remains after the residence-v2 physical-home chunk and commute safety fixes. Residence audits 14 and 16 both reached roughly 130 of 178 released exterior/residence tickets before a 60-second server tick watchdog stopped the server.

The exterior builder now emits CI-only request/start/complete markers for every transient chunk. The next fresh-world residence audit must use those markers to identify whether the stall occurs while requesting/generating a chunk, creating its incremental plan, applying that plan, validating the physical residence, or releasing/observing the chunk.

This diagnostic does not change gameplay state, generation layout, save data or ticket policy. It exists only to close the deterministic watchdog with evidence rather than by reducing content or disabling validation.
