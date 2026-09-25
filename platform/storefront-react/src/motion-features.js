// Motion's animation engine, loaded as a separate chunk after the first paint (see LazyMotion in App.jsx).
// domMax = animations + gestures + layout/shared-layout animations (needed for the gliding grid and pills).
import { domMax } from 'motion/react';

export default domMax;
