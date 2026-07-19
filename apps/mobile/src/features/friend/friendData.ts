export type FriendProgress = {
  dataStructure: number;
  algorithm: number;
  operatingSystem: number;
  database: number;
  computerArchitecture: number;
  network: number;
};

export type Friend = {
  id: string;
  nickname: string;
  school: string;
  department: string;
  level: number;
  streak: number;
  attendance: string;
  isOnline: boolean;
  avatarColor: string;
  avatarAccentColor: string;
  progress: FriendProgress;
};

export const FRIENDS: Friend[] = [
  {
    id: "minji",
    nickname: "민지",
    school: "서울과학기술대학교",
    department: "컴퓨터공학과",
    level: 15,
    streak: 12,
    attendance: "오늘 출석 완료",
    isOnline: true,
    avatarColor: "#E9E3FF",
    avatarAccentColor: "#6D5DFB",
    progress: {
      dataStructure: 75,
      algorithm: 55,
      operatingSystem: 40,
      database: 65,
      computerArchitecture: 30,
      network: 50,
    },
  },
  {
    id: "junseo",
    nickname: "준서",
    school: "서울과학기술대학교",
    department: "컴퓨터공학과",
    level: 15,
    streak: 12,
    attendance: "오늘 출석 완료",
    isOnline: true,
    avatarColor: "#DDF6ED",
    avatarAccentColor: "#12B981",
    progress: {
      dataStructure: 60,
      algorithm: 82,
      operatingSystem: 45,
      database: 52,
      computerArchitecture: 67,
      network: 73,
    },
  },
  {
    id: "seoyeon",
    nickname: "서연",
    school: "서울과학기술대학교",
    department: "컴퓨터공학과",
    level: 15,
    streak: 12,
    attendance: "오늘 출석 완료",
    isOnline: true,
    avatarColor: "#FFE8DE",
    avatarAccentColor: "#F97316",
    progress: {
      dataStructure: 48,
      algorithm: 65,
      operatingSystem: 70,
      database: 38,
      computerArchitecture: 56,
      network: 44,
    },
  },
  {
    id: "hyeonwoo",
    nickname: "현우",
    school: "서울과학기술대학교",
    department: "컴퓨터공학과",
    level: 14,
    streak: 8,
    attendance: "어제 출석 완료",
    isOnline: false,
    avatarColor: "#E1F1FF",
    avatarAccentColor: "#3B82F6",
    progress: {
      dataStructure: 44,
      algorithm: 70,
      operatingSystem: 32,
      database: 61,
      computerArchitecture: 40,
      network: 58,
    },
  },
  {
    id: "jiho",
    nickname: "지호",
    school: "서울과학기술대학교",
    department: "컴퓨터공학과",
    level: 15,
    streak: 12,
    attendance: "어제 출석 완료",
    isOnline: false,
    avatarColor: "#F7E5FF",
    avatarAccentColor: "#C241E8",
    progress: {
      dataStructure: 50,
      algorithm: 42,
      operatingSystem: 58,
      database: 78,
      computerArchitecture: 46,
      network: 68,
    },
  },
  {
    id: "yuna",
    nickname: "유나",
    school: "서울과학기술대학교",
    department: "컴퓨터공학과",
    level: 13,
    streak: 5,
    attendance: "2일 전 출석 완료",
    isOnline: false,
    avatarColor: "#FFF0CC",
    avatarAccentColor: "#F59E0B",
    progress: {
      dataStructure: 35,
      algorithm: 60,
      operatingSystem: 52,
      database: 45,
      computerArchitecture: 63,
      network: 30,
    },
  },
];

export const getFriend = (friendId?: string) =>
  FRIENDS.find((friend) => friend.id === friendId) ?? FRIENDS[0];
