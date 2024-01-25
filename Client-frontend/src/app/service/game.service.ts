import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class GameService {

  private readonly url = '/api/game'

  constructor(private http: HttpClient) {
  }

  public create(value): any {
    return this.http.post(`${this.url}/create`, value);
  }

  public connectToGame(value, gameUid: string): any  {
    return this.http.post(`${this.url}/connect`, {player: value, gameUid: gameUid});
  }

  public connectToRandom(value): any {
    return this.http.post(`${this.url}/connect/random`, value);
  }
}
