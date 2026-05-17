import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForecastCard } from './forecast-card';

describe('ForecastCard', () => {
  let component: ForecastCard;
  let fixture: ComponentFixture<ForecastCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ForecastCard],
    }).compileComponents();

    fixture = TestBed.createComponent(ForecastCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
