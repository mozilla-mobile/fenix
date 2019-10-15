REVIEWER NOTE: this is a first draft. Content will likely change, so only a minimal effort at formatting was made.


## Architecture Overview

### Unidirectional data flow

Firefox Preview's presentation layer architecture is based on the concept of "unidirectional data flow." This is a popular approach in client side development, especially on the web, and is core to Redux, MVI, Elm Architecture, and Flux.  Our architecture is not identical to any of these (and they are not identical to each other), but the base concepts are the same. For a basic understanding of the motivations and approach, see [the official Redux docs](https://redux.js.org/basics/data-flow).  For an article on when unidirectional data flow is and is not a good approach, see [this](https://medium.com/swlh/the-case-for-flux-379b7d1982c6). These are both written from the perspective of React.js developers, but the concepts are largely the same.

Our largest deviation from these architectures is that while they each recommend one large, global store of data, we have a single store per screen. This carries both benefits and drawbacks, both of which will be covered later in this document.

### Important objects

#### <a name="store"/>Store
##### Details
A store of state

See mozilla.components.lib.state.Store
Pushes changes to: [View](#view)
Called by: [Controller](#controller)

##### Description
Maintains a [State](#state) object and a [Reducer](#reducer). Whenever the Store receives a new [Action](#action) via `store.dispatch(action)`, it calls the [Reducer](#reducer) with the previous state and the new action. The result is then stored as the new State, and published to all consumers of the store.

It is recommended that consumers rely as much as possible on `consumeFrom(store)`, rather than querying `store.state`. `consumeBy` is called any time state is updated, ensuring that the most up to date data is always used. This can prevent subtle bugs around call order.

Note that there is one Store for any given screen, and only one will be active at any given time. Stores are persisted across configuration changes, but created and destroyed during fragment transactions. This means that data that must be shared across Stores must be passed as arguments to the new fragment.

#### <a name="action"/>Action
##### Details
Simple description of a state change

See mozilla.components.lib.state.Action
Created by: [Controller](#controller)
Is pushed to: [Store](#store)

##### Description
Simple data object that carries information about a [State](#state) change to a [Store](#store).  An Action describes _something that happened_, and carries any data relevant to that change. For example, `AccountSettingsFragmentAction.SyncFailed(val time: Long)`, which describes a sync that failed at a specific time.

#### <a name="state"/>State
##### Details
Description of the state of a screen

See mozilla.components.lib.state.State
Referenced by: [Store](#store)

##### Description
Simple, immutable data object that contains all of the backing data required to display a screen. This does not include style details like colors and view sizes, which are handled by the [View](#view).

As much as possible, the State object should be an accurate, 1:1 representation of what is actually shown on the screen. That is to say, if the same State object were emitted twice, the screen would look exactly the same both times, regardless of how the screen previously looked. This is not always possible as Android UI elements are very stateful, but it is a good goal to aim for.

One major benefit of rendering a screen based on a State object is its impact on testing. UI tests are notoriously difficult to build and maintain. We try to build a very simple, dumb [View](#view) that accurately renders a State object. If we can trust the View to be correct, that allows us to test our UI by verifying the correctness of our State object.

This also gives us a major advantage when debugging. If the UI looks wrong, check the State object. If it's correct, the problem is in the View binding. If not, check that the correct [Action](#action) was sent. If so, the problem is in the reducer. If not, check the [Controller](#controller) that sent the Action. This helps us quickly narrow down problems.

#### <a name="reducer"/>Reducer
##### Details
Pure function used to create new [State](#state) objects

See mozilla.components.lib.state.Reducer
Referenced by: [Store](#store)

##### Description
A function that accepts the previous State and an [Action](#action), then combines them in order to return the new State. It is important that all Reducers remain [pure](https://en.wikipedia.org/wiki/Pure_function). This allows us to test Reducers based only on their inputs, without requiring that we take into account the state of the rest of the app.

Note that the Reducer is always called serially, as state could be lost if it were ever executed in parallel.

#### <a name="interactor"/>Interactor
##### Details
Called in response to a direct user action. Delegates to something else

Called by: [View](#view)
Calls: [Controllers](#controller), other Interactors

##### Description
This is the first object called whenever the user performs an action. Typically this will result in code in the [View](#view) that looks something like `some_button.onClickListener { interactor.onSomeButtonClicked() } `. It is the Interactors job to delegate this button click to whichever object should handle it.

Interactors may hold references to multiple other Interactors and Controllers, in which case they delegate specific methods to their appropriate handlers. This helps prevent bloated Controllers that both perform logic and delegate to other objects.

Sometimes an Interactor will only reference a single Controller. In these cases, the Interactor will simply forward calls to equivalent calls on the Controller. The Interactor does very little in these cases, and exists only to be consistent with the rest of the app.

#### <a name="controller"/>Controller
##### Details
Determines how the app should be updated whenever something happens

Called by: [Interactor](#interactor)
Calls: [Store](#store), library code (e.g., forward a back-press to Android, trigger an FxA login, navigate to a new Fragment, use an Android Components UseCase, etc)

##### Description
This is where much of the business logic of the app lives. Whenever called by an Interactor, a Controller will do one of the three following things:
- Create a new [Action](#action) that describes the necessary change, and send it to the Store
- Navigate to a new fragment via the NavController. Optionally include any state necessary to create this new fragment
- Interact with some third party manager. Typically these will update their own internal state and then emit changes to some observer, which will be used to update our Store

Controllers can become very complex, and should be unit tested thoroughly whenever their methods do more than delegate simple calls to other objects.

#### <a name="view"/>View
##### Details
Initializes UI elements, then updates them in response to [State](#state) changes

Observes: [Store](#store)
Calls: [Interactor](#interactor)

##### Description
The view defines the mapping of State to UI. This includes initial setup of Views, and also typically includes an `update(state)` function that is called whenever State is changed.

Views should be as dumb as possible, and should include little or no conditional logic. Ideally, each primitive value in a State object is set on some field of a UI element, with no other logic included.

Views set listeners on to UI elements, which trigger calls to one or more Interactors. 

REVIEWER NOTE: above this point is a first draft. Below is still an outline

### Important notes
- Unlike other common implementations of unidirectional data flow, which typically have one global Store of data, we maintain a smaller Store for each screen.
- Stores and their State are persisted across configuration changes via an Architecture Components ViewModel.
- Whenever a fragment newly created or finished (by the ViewModel definition), its Store will also be created/destroyed
- Communication between Stores only happens by loading the appropriate data into arguments for the new fragment, which uses them to construct the initial state of the new Store.
  - We currently violate this in a few places, but are actively refactoring out those violations

## Simplified Example (section in progress)

(this will be a lot of work, but imo worth it. reading through a fully fleshed out section of the 
code base is hard. including this will help to onboard contributors and new devs in a simpler 
context)

- simple, drawn example
- text message app
  - contact list screen + conversation screen
  - when on contact screen:
    - TODO include multiple controllers to show what an interactor does
    - updating a contact changes the state
    - selecting a conversation routes to a new screen
  - when on conversation screen
    - backing out routes to a new screen + sends an extra to update 'unread' status for the conversation


## Known Limitations (section in progress)

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
