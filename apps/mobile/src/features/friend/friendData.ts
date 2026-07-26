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

export const MAX_FRIEND_REQUESTS = 30;

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

export const FRIEND_REQUESTS: Friend[] = [
  {
    id: "dohyun",
    nickname: "도현",
    school: "서울과학기술대학교",
    department: "컴퓨터공학과",
    level: 12,
    streak: 9,
    attendance: "오늘 출석 완료",
    isOnline: true,
    avatarColor: "#E7EEFF",
    avatarAccentColor: "#4F77E8",
    progress: {
      dataStructure: 42,
      algorithm: 64,
      operatingSystem: 51,
      database: 37,
      computerArchitecture: 58,
      network: 45,
    },
  },
  {
    id: "sumin",
    nickname: "수민",
    school: "서울과학기술대학교",
    department: "컴퓨터공학과",
    level: 11,
    streak: 6,
    attendance: "오늘 출석 완료",
    isOnline: false,
    avatarColor: "#FFE7F0",
    avatarAccentColor: "#E85A8E",
    progress: {
      dataStructure: 51,
      algorithm: 39,
      operatingSystem: 66,
      database: 44,
      computerArchitecture: 35,
      network: 59,
    },
  },
];

export const FRIEND_SEARCH_RESULTS: Friend[] = [
  {
    id: "taeyoung",
    nickname: "태영",
    school: "서울과학기술대학교",
    department: "컴퓨터공학과",
    level: 16,
    streak: 21,
    attendance: "오늘 출석 완료",
    isOnline: true,
    avatarColor: "#E3F7F0",
    avatarAccentColor: "#1BAA7A",
    progress: {
      dataStructure: 76,
      algorithm: 68,
      operatingSystem: 62,
      database: 55,
      computerArchitecture: 49,
      network: 73,
    },
  },
  {
    id: "eunji",
    nickname: "은지",
    school: "서울과학기술대학교",
    department: "컴퓨터공학과",
    level: 10,
    streak: 4,
    attendance: "어제 출석 완료",
    isOnline: false,
    avatarColor: "#FFF3D7",
    avatarAccentColor: "#D18A13",
    progress: {
      dataStructure: 34,
      algorithm: 47,
      operatingSystem: 39,
      database: 65,
      computerArchitecture: 43,
      network: 51,
    },
  },
];
