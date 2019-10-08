## Architecture Overview

### Unidirectional data flow
- unidirectional data flow
  - _very_ short description of what this means (a few sentences)
  - link to better description
  - roughly analagous to (list other common implementations)
    - there are differences between them, but they share the same underlying concept
    - note that we have many smaller stores, instead of one big one

### Important objects
- description of individual object types
  - for each one:
    - description
      - as much as possible, link to kdocs rather than duplicating
      - some additional information about how they fit into the larger whole
    - this object reacts to updates from X
    - this object notifies Y of changes
    - where valuable, relationships (1:1, 1:1+, 1:many)
  - objects
    - Store
      - see AC docs
      - store:fragment == 1:1
      - store:reducer == 1:1
      - only one will ever be alive at one time
    - Action
      - see AC docs
    - State
      - see AC docs
      - immutable
      - as much as possible, 1:1 relationship with UI
        - explain why
        - stress that _this makes testing easier_
    - Reducer
      - see AC docs
      - must be a pure function
      - where the bulk of 'logic' should live
    - Controller
      - when called by Interactor, collects context and does one (and _only_ one) of the following:
        - send Action to Store
        - route to new Fragment
          - optionally including data passed as intent extras
        - update some third party code (e.g., trigger FxA login, use an AC use case, forward a back press to Android, etc)
    - Interactor
      - fragment:interactor == 1:1+
      - interactor:controller == 1:0+
      - interactor:interactor == 1:0+
      - delegates user actions to Controllers or other Interactors
      - this layer exists to
        - keep the View from having to know what objects are listening to it
        - keep controllers from having to handle routing calls to correct objects + creating Actions
    - View
      - fragment:view == 1:1+
      - view:interactor == 1:1
      - defines mapping of state -> UI
      - updates views whenever a new state obj is received
  
### Important notes
- controller changes can happen either as an update to the store, or by routing to a new fragment + store
- communication between stores only happens via transactions 
- stores & reducers are persisted across configuration changes (via Arch Components VM), but created / destroyed during fragment transactions
  - everything else is destroyed / recreated during configuration changes 
  


## Simplified Example

(this will be a lot of work, but imo worth it. reading through a fully fleshed out section of the 
code base is hard. including this will help to onboard contributors and new devs in a simpler 
context)

- simple, drawn example
- text message app
  - contact list screen + conversation screen
  - when on contact screen:
    - updating a contact changes the state
    - selecting a conversation routes to a new screen
  - when on conversation screen
    - backing out routes to a new screen + sends an extra to update 'unread' status for the conversation


## Known Limitations

- how to handle certain things is not defined in current architecture
  - how do we update state from outside of the activity lifecycle (i.e., when no store is active)?
    - e.g., setup in application
  - how do we handle values that need to be set at a higher scope, and shared by all?
    - e.g., settings
- state update / routing change distinction is sometimes fuzzy
  - anything that involves changing screens goes through a different state update flow
    - handled by passing values to a new fragment
- reducer / controller responsibilities can become muddied
  - state updates need to go through the store, android calls need to happen in controller
    - what happens if code needs to conditionally do either?
      - it is forced to do that logic in the controller
      - we would prefer to keep as much logic as possible in the reducer
- most interactors do nothing except forward to controllers
  - these can still provide some value in making changes that add extra dependencies simpler
  - most of the time though, this layer does not seem to be useful

