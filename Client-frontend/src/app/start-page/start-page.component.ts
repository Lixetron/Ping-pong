import {Component, OnInit} from '@angular/core';
import {Router, RouterOutlet} from '@angular/router';
import {MatFormField, MatInput, MatLabel} from '@angular/material/input';
import {MatButton} from '@angular/material/button';
import {FormsModule} from '@angular/forms';
import {GameService} from '../service/game.service';
import {FieldComponent} from '../field/field.component';

@Component({
  selector: 'app-start-page',
  standalone: true,
  imports: [
    RouterOutlet,
    MatInput,
    MatButton,
    MatFormField,
    MatLabel,
    FormsModule,
    FieldComponent
  ],
  templateUrl: './start-page.component.html',
  styleUrl: './start-page.component.css'
})
export class StartPageComponent implements OnInit {
  player: string;
  gameUid: string;

  constructor(private gameService: GameService, private router: Router) {
  }

  findRandomGame(): void {
    localStorage.setItem('player', this.player);
    this.gameService.connectToRandom({name: this.player}).subscribe(
      (value: { uid: string; }) => this.gameUid = value.uid);
  }

  findGameByUid(uid: string): void {
    localStorage.setItem('player', this.player);
    this.gameService.connectToGame(this.player, uid).subscribe(
      {
        next: () => {
          this.gameUid = uid;
        },
        error: () => {
          console.error('err')
        }
      }
    )
  }

  createGame(): void {
    localStorage.setItem('player', this.player);
    this.gameService.create({name: this.player}).subscribe(
      (value: { uid: string; }) => this.gameUid = value.uid);
  }

  ngOnInit(): void {
    this.player = localStorage.getItem('player');
  }
}
