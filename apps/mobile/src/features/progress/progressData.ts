export type ProgressItem = {
  title: string;
  shortTitle: string;
  percent: number;
};

export const progressItems: ProgressItem[] = [
  { title: "Data Structure", shortTitle: "DS", percent: 85 },
  { title: "Algorithm", shortTitle: "Algo", percent: 100 },
  { title: "Network", shortTitle: "Net", percent: 62 },
  { title: "Database", shortTitle: "DB", percent: 34 },
  { title: "Operating System", shortTitle: "OS", percent: 8 },
  { title: "Computer Architecture", shortTitle: "Arch", percent: 76 },
];
