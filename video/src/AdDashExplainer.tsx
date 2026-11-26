import React from "react";
import { Audio, Sequence, staticFile, useCurrentFrame } from "remotion";
import timeline from "./timeline.json";
import { StudioLayout } from "./components/StudioLayout";
import { Scene01Intro } from "./scenes/Scene01Intro";
import { Scene02Refresh } from "./scenes/Scene02Refresh";
import { Scene03Bubble } from "./scenes/Scene03Bubble";
import { Scene04Fullscreen } from "./scenes/Scene04Fullscreen";
import { Scene05TodayGoals } from "./scenes/Scene05TodayGoals";
import { Scene06AnalyticsWallet } from "./scenes/Scene06AnalyticsWallet";
import { Scene07Outro } from "./scenes/Scene07Outro";

export const AdDashExplainer: React.FC = () => {
  const frame = useCurrentFrame();
  const totalFrames = timeline.totalFrames;
  const globalProgress = frame / Math.max(1, totalFrames);

  const accentMap: Record<string, string> = {
    "01_intro": "#D9252A",
    "02_refresh": "#10B981",
    "03_bubble": "#F59E0B",
    "04_fullscreen": "#3B82F6",
    "05_today_goals": "#3B82F6",
    "06_analytics_wallet": "#D9252A",
    "07_outro": "#D9252A",
  };

  return (
    <>
      {/* Mastered neural voiceover + sidechain-ducked ambient music */}
      <Audio src={staticFile("audio/master.mp3")} />

      {timeline.chapters.map((ch, idx) => {
        const showHeader = ch.id !== "01_intro" && ch.id !== "07_outro";
        return (
          <Sequence
            key={ch.id}
            from={ch.startFrame}
            durationInFrames={ch.durationFrames}
          >
            {/* Subtle transition whoosh on chapters 2..7 */}
            {idx > 0 && (
              <Sequence from={0} durationInFrames={30}>
                <Audio src={staticFile("audio/whoosh.wav")} volume={0.32} />
              </Sequence>
            )}

            <StudioLayout
              badge={showHeader ? ch.badge : undefined}
              title={showHeader ? ch.title : undefined}
              accentColor={accentMap[ch.id] || "#D9252A"}
              captions={ch.captions}
              globalProgress={globalProgress}
            >
              {ch.id === "01_intro" && <Scene01Intro />}
              {ch.id === "02_refresh" && <Scene02Refresh />}
              {ch.id === "03_bubble" && (
                <Scene03Bubble durationFrames={ch.durationFrames} />
              )}
              {ch.id === "04_fullscreen" && <Scene04Fullscreen />}
              {ch.id === "05_today_goals" && <Scene05TodayGoals />}
              {ch.id === "06_analytics_wallet" && (
                <Scene06AnalyticsWallet durationFrames={ch.durationFrames} />
              )}
              {ch.id === "07_outro" && <Scene07Outro />}
            </StudioLayout>
          </Sequence>
        );
      })}
    </>
  );
};
