import React from "react";
import { Composition } from "remotion";
import timeline from "./timeline.json";
import { AdDashExplainer } from "./AdDashExplainer";

export const RemotionRoot: React.FC = () => {
  return (
    <>
      <Composition
        id="AdDashExplainer"
        component={AdDashExplainer}
        durationInFrames={timeline.totalFrames}
        fps={timeline.fps}
        width={timeline.width}
        height={timeline.height}
      />
    </>
  );
};
