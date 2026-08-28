import { httpClient } from "../../shared/api/httpClient";
import type { Inventory } from "./inventoryTypes";

export const inventoryApi = {
  getInventory(accessToken: string): Promise<Inventory> {
    return httpClient.get<Inventory>("/api/avatar/inventory", { accessToken });
  },
};
